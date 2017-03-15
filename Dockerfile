FROM openjdk:7
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
ENV SIMTYPE 1

RUN javac Simulation.java

CMD ["sh", "-c", "java Simulation ${SIMTYPE}"]
