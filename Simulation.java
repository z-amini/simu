import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;

public class Simulation {	
    private static final String OUTPUTFILE = "/usr/output/92106345.csv";
    private static int simuRound = 500;
    private static Random rand1 = new Random(System.currentTimeMillis());
    private static Random rand2 = new Random(System.currentTimeMillis());
    private static Random rand3 = new Random(System.currentTimeMillis());
    private static int time = 0;

    public static void main(String[] args) {
		int SelectedAlgo = Integer.valueOf(args[0]);
        System.out.println("Selected Algorithm is " +  SelectedAlgo );
        
    	PriorityQueue<Event> eventQueue = new PriorityQueue<>(100, new Comparator<Event>() {

			@Override
			public int compare(Event e0, Event e1) {
				return e0.getTime() - e1.getTime();
			}
		});
    	
    	ArrayList<RequestLog> requests = new ArrayList<>();
        eventQueue.add(new Event(0, EventType.Arrival));
        
        Server[] servers = new Server[5];
        for (int i = 0; i < servers.length; i++) {
			servers[i] = new Server();
		}
        
        while ( eventQueue.size() > 0 ) {
        	Event e = eventQueue.poll();
        	time = e.getTime();
        	if (e.getType() == EventType.Arrival) {
        		int serviceTime = rand2.nextInt(30) + 10;
            	Task t = new Task(time, serviceTime);
            	//task to server
            	int serverIndex = decideServer(servers, SelectedAlgo);
            	int depTime = servers[serverIndex].addTask(t, time);
            	if (depTime > 0) {
            		e = new Event(depTime, EventType.Departure);
            		e.setServer(serverIndex);
            		eventQueue.add(e);
            	}
            	requests.add(new RequestLog(t, serverIndex));
            	if (requests.size() < simuRound) {
                	int arrivalInterval = rand1.nextInt(19) + 1;
                	e = new Event(time + arrivalInterval, EventType.Arrival);
                	eventQueue.add(e);
            	}
        	}
        	else {
    			int serverIndex = e.getServer();
        		int depTime = servers[serverIndex].endTask(time);
        		if (depTime > 0) {
            		e = new Event(depTime, EventType.Departure);
            		e.setServer(serverIndex);
            		eventQueue.add(e);
            	}
        	}
        	
		}
        
        double meanResponseTime = 0;
        for (RequestLog r : requests) {
			meanResponseTime += r.getRequest().getDepartureTime()
					- r.getRequest().getServiceTime() - r.getRequest().getArrivalTime();
		}
        meanResponseTime /= requests.size();
        write(meanResponseTime+"\n");
        for (int i = 0; i < servers.length; i++) {
        	write( (i==0? "" : ", ") + servers[i].getWorkingRate(time));
		}
        for (RequestLog r : requests) {
			write("\n" + r.getRequest().getArrivalTime() +
					", " + r.getRequest().getDepartureTime() +
					", " + r.getRequest().getServiceTime() +
					", " + (r.getServer() + 1));
		}
    }
    
    private static int decideServer(Server[] servers, int algorithm) {
    	ArrayList<Integer> candidates;
		switch (algorithm) {
		case 1:
			return rand3.nextInt(servers.length);
		case 2:
			candidates = minWaitingTimes(servers);
			return candidates.get(rand3.nextInt(candidates.size()));
		case 3:
			candidates = randomMinWaitingTimes(servers);
			return candidates.get(rand3.nextInt(candidates.size()));
		}
		return 0;
	}

	private static ArrayList<Integer> randomMinWaitingTimes(Server[] servers) {
		int[] index = {rand3.nextInt(servers.length), rand3.nextInt(servers.length)};
		Server[] selectedServers = {servers[index[0]], servers[index[1]]};
		ArrayList<Integer> result = minWaitingTimes(selectedServers);
		for (int i = 0; i < result.size(); i++) {
			result.set(i, index[result.get(i)]);
		}
		return result;
	}

	private static ArrayList<Integer> minWaitingTimes(Server[] servers) {
		ArrayList<Integer> candidates = new ArrayList<>();
		candidates.add(0);
		for (int i = 1; i < servers.length; i++) {
			if (servers[i].waitingTime(time) < servers[candidates.get(0)].waitingTime(time)) {
				candidates.clear();
				candidates.add(i);
			}
			else if (servers[i].waitingTime(time) == servers[candidates.get(0)].waitingTime(time)) {
				candidates.add(i);
			}
		}
		return candidates;
	}

	private static void write(String content) {
    	try (final BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUTFILE))){
            bw.write(content);
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    	
    }
}

class Server {

	private ArrayList<Task> waitingQueue = new ArrayList<>();
	private Task processingTask;
	private int workingTime = 0;
	
	public int addTask(Task task, int time) {
		if (processingTask == null) {
			processingTask = task;
			processingTask.setDepartureTime(time);
			return time + task.getServiceTime();
		}
		else {
			waitingQueue.add(task);
			return -1;
		}
	}
	
	public int endTask (int time) {
		workingTime += processingTask.getServiceTime();
		if (waitingQueue.size() == 0) {
			processingTask = null;
			return -1;
		}
		else {
			processingTask = waitingQueue.get(0);
			processingTask.setDepartureTime(time);
			waitingQueue.remove(0);
			return time + processingTask.getServiceTime();
		}
	}
	
	public int departureEventTime() {
		return processingTask.getDepartureTime();
	}
	
	public int waitingTime (int time) {
		if (processingTask == null)
			return 0;
		int result = processingTask.getDepartureTime() - time;
		for (Task task : waitingQueue) {
			result += task.getServiceTime();
		}
		return result;
	}
	
	public double getWorkingRate(int time) {
		return (double) workingTime / time;
	}
}


class Task {
	
	private int arrivalTime, serviceTime, departureTime;
	
	public Task(int arrivalTime, int serviceTime) {
		this.arrivalTime = arrivalTime;
		this.serviceTime = serviceTime;
	}
	
	public void setDepartureTime(int currentTime) {
		this.departureTime = currentTime + serviceTime;
	}
	
	public int getDepartureTime() {
		return departureTime;
	}
	
	public int getArrivalTime() {
		return arrivalTime;
	}
	
	public int getServiceTime() {
		return serviceTime;
	}
}

class Event {
	private Integer time, server;
	private EventType type;
	
	public Event(int time, EventType type) {
		this.time = time;
		this.setType(type);
	}
	
	public int getTime() {
		return time;
	}

	public int getServer() {
		return server;
	}

	public void setServer(int server) {
		this.server = server;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}
}

class RequestLog {
	private Task request;
	private int server;
	
	public RequestLog(Task request, int server) {
		this.request = request;
		this.server = server;
	}
	
	public Task getRequest() {
		return request;
	}
	
	public int getServer() {
		return server;
	}
}

enum EventType {
	Arrival, Departure;
}