package model;


import javafx.animation.Timeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static model.Body.G;
import static model.CelestialBody.EARTH;
import static model.CelestialBody.TITAN;

public class BodySystem {
    private static final int SEC_IN_MINUTE = 60;
    private static final int SEC_IN_HOUR = SEC_IN_MINUTE * 60;
    private static final int SEC_IN_DAY = SEC_IN_HOUR * 24;
    private static final int SEC_IN_YEAR = 31556926;
    private long elapsedSeconds = 0;

    //the time when I start to adjust the trajectory with the engines
    private long saturnTime = 238608000;
    private long closestToSaturn = 238996800;
    private long stayInOrbitSaturn = 238999200;
    private double xSaturn =1407803030373.561800;
    private double ySaturn = -290357895704.930240;
    private double zSaturn = -51070777398.352970;

    private long timeClosestToTitan = 239000400;
    private double xTitan = 1409198048006.711000;
    private double yTitan = -287772211103.425350;
    private double zTitan = -52532091692.418510;
    private PIDController controller = new PIDController(1200,xTitan,yTitan);

    private final double maximumIonThrusterForce = 5;



    private PIDController velocityx = new PIDController(1200,1,0,0);
    private PIDController velocityy = new PIDController(1200,1,0,0);
    private PIDController velocityz = new PIDController(1200,1,0,0);


    private ArrayList<Vector3D> PIDoutput = new ArrayList<>();

    public double startingDistance = 1.4E+10;
    public Body chosenOne;
    public String currentTime;

    public double minDistanceToTarget = Double.MAX_VALUE;

    //8.862395206670821E8
    //2.223667170921659E9
    // from titan 3.2670147095408136E8
    //6.874331296673225E8
    // 6.874331296673225E8
    //4.822473458108047E7

    private long firstLaunch = 185238000 + SEC_IN_DAY/8;
    private long intervalTime = (long) SEC_IN_DAY/8;  //how long between launches
    private long interval = firstLaunch;
    private long timeToLaunch;
    private boolean launched = false;

    private long perfectTime; //change this to date format once we can

    private long minDistance = Long.MAX_VALUE;
    private int minDistanceProbe = -1;


    Timeline timeline;

    public double currentDistance;

    private int probesNum = 1; //number of probes we send at a time
    private int probesLimit =1 ; //limit the number of probes we send
    private ArrayList<Probe> realProbesList = new ArrayList<>();
    private ArrayList<Double> minDistancesList = new ArrayList<>();
    ArrayList<Long> launchTimesList = new ArrayList<>();//stores the times in which we launch the probes, we should change this to a date in the future

    private List<Body> bodies;

    private int destination; //0 = titan, 1 = earht

    public BodySystem(int destination) {
        bodies = new ArrayList<>();
        this.destination = destination;
    }

    public BodySystem(int destination, long elapsedSeconds, long firsLaunch){
        this.destination = destination;
        this.elapsedSeconds = elapsedSeconds;
        this.firstLaunch = firsLaunch;
    }

    public List<Body> getBodies() {
        return bodies;
    }

    public void addBody(Body body) {
        bodies.add(body);
    }

    public double update(double timeSlice, Timeline timeline) {
        //System.out.println(getElapsedTimeAsString());
        this.timeline = timeline;
        // reset acceleration so we can accumulate acceleration due to gravitation from all bodies
        bodies.stream().forEach(i -> i.resetAcceleration()) ;

        // add gravitation force to each body from each body
        for (int i = 0; i < bodies.size(); i++) {
            Body current = bodies.get(i);
            for (int j = i+1; j < bodies.size(); j++) {
                Body other = bodies.get(j);
                current.addAccelerationByGravityForce(other);
                other.addAccelerationByGravityForce(current);
            }
        }

        elapsedSeconds += timeSlice;
        



        if (realProbesList.size()>=1) {
            //distance to start checking how close the probe is to titan
            //if there is no output increase the distance when to check
            if(destination == 0){
                double distance = realProbesList.get(0).location.probeDistance(bodies.get(10).location);
                checkLocationtoTitan(bodies.get(10));
            }else{
                checkLocationtoTitan(bodies.get(3));
            }

        }

        //THIS IF STATEMENT DOESNT WORK ANYMORE
        if (realProbesList.size()>=1 && elapsedSeconds>=timeClosestToTitan && destination == 0){

            double xVel = velocityx.compute(realProbesList.get(0).velocity.x,bodies.get(10).velocity.x);
            double yVel = velocityy.compute(realProbesList.get(0).velocity.y,bodies.get(10).velocity.y);
            double zVel = velocityz.compute(realProbesList.get(0).velocity.z,bodies.get(10).velocity.z);

            Vector3D velocityVector = new Vector3D(xVel,yVel,zVel);

            realProbesList.get(0).addAccelerationByForce(velocityVector);

            realProbesList.get(0).calculateFuelConsumption(velocityVector, timeSlice);

        }



        // update velocity and location for each body
        bodies.stream().forEach(i -> i.updateVelocityAndLocation(timeSlice));
        //157786200

        if(elapsedSeconds > interval  && !launched && realProbesList.size() <=probesLimit){
            launchNewProbe();
            launched = true;
        }



        return timeSlice;
    }



    /**
     * launches a new probe, with earth's location. in this method you can choose its velocity, mass etc.
     */
    private void launchNewProbe(){

        //System.out.println(destination);
        //info for the probe
        double radius = 100; //in meters
        double mass = 5000; //in kg

        //launch multiple probes at same time but different velocities
        for(int x = 0; x <= probesNum-1; x++){


        }

        Vector3D location;
        Vector3D velocity;

        if(destination == 1){
            //location = bodies.get(10).location;//bodies.get(3).getLocation();//EARTH.getAsBody().location;//because we are always launching from same site
            velocity = TITAN.getAsBody().velocity; //bodies.get(3).getVelocity();//EARTH.getAsBody().velocity; //just as a reference for now, change to something more realistic
            velocity.x = velocity.x*-1;
            Vector3D v = new Vector3D(6371.008E+3,0,0);
            v.add(bodies.get(10).location);
            location = v;

        }else{
            //location = bodies.get(3).location;//bodies.get(3).getLocation();//EARTH.getAsBody().location;//because we are always launching from same site
            velocity = EARTH.getAsBody().velocity; //bodies.get(3).getVelocity();//EARTH.getAsBody().velocity; //just as a reference for now, change to something more realistic
            Vector3D v = new Vector3D(6371.008E+3,0,0);
            v.add(bodies.get(3).location);
            location = v;
        }

        // creates random scalars for probes, for optimization purposes.
        double rangeMax = 2.75;
        double rangeMin = 1.5;
        Random rand = new Random();
        double scalarX = rangeMin +(rangeMax-rangeMin)*rand.nextDouble();
        double scalarY = rangeMin +(rangeMax-rangeMin)*rand.nextDouble();

        // X and Y velocity of the probe.
        velocity.x = velocity.x*1.978;
        velocity.y = velocity.y*1.745;



        Probe rProbe = new Probe(location,velocity,radius,mass,"prob" + realProbesList.size(),elapsedSeconds,
                velocity.x,velocity.y);

        realProbesList.add(rProbe);
        minDistancesList.add(Double.MAX_VALUE);
        rProbe.name = "probe" + realProbesList.indexOf(rProbe);
        System.out.println(rProbe.name + " launched on " + getElapsedTimeAsString());
        launchTimesList.add(elapsedSeconds);

        addBody(rProbe);
        //System.out.println("probe launched");
        //change this so it doesn't send multiple probes at one time
        interval+=intervalTime;
    }


    /**
     * checks the distance between each probe and titan, will print the probe that gets closest to titan.
     */
    public double checkLocationtoTitan(Body other){
        startingDistance = 5E15;
        double distance = realProbesList.get(0).location.probeDistance(other.location); // location of the probe, distance to Targer
        //System.out.println(distance);
        if (distance<startingDistance){
            chosenOne = realProbesList.get(0);
            //System.out.println(realProbesList.get(0));
            //System.out.println(other);
            startingDistance = distance;
            //System.out.println(" The minimum distance is(in meters) : " + startingDistance);
            currentTime = getElapsedTimeAsString();
            //System.out.println(currentTime);
            //System.out.println(elapsedSeconds);
            minDistanceToTarget = startingDistance;
            return startingDistance;
        }
        return startingDistance;

    }



    public Optional<Body> getBody(String name) {
        return bodies.stream().filter(i -> i.name.equals(name)).findFirst();
    }

    public String getElapsedTimeAsString() {
        long years = elapsedSeconds / SEC_IN_YEAR;
        long days = (elapsedSeconds % SEC_IN_YEAR) / SEC_IN_DAY;
        long hours = ( (elapsedSeconds % SEC_IN_YEAR) % SEC_IN_DAY) / SEC_IN_HOUR;
        long minutes = ( ((elapsedSeconds % SEC_IN_YEAR) % SEC_IN_DAY) % SEC_IN_HOUR) / SEC_IN_MINUTE;
        long seconds = ( ((elapsedSeconds % SEC_IN_YEAR) % SEC_IN_DAY) % SEC_IN_HOUR) % SEC_IN_MINUTE;
        return String.format("Years:%04d, Days:%03d, Hours:%02d, Minutes:%02d, Seconds:%02d", years, days, hours, minutes, seconds);
    }

    public long getElapsedTime(){
        return elapsedSeconds;
    }

    public long getTimeToLaunch() {
        if(firstLaunch > elapsedSeconds){
            timeToLaunch = firstLaunch - elapsedSeconds;
            return timeToLaunch;
        }else{
            return -1;
        }
    }

    public void setFirstLaunch(long firstLaunch) {
        this.firstLaunch = firstLaunch;
        //interval = firstLaunch;
    }

    public void setInterval(long interval){
        this.interval = interval;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    /**
     * adjust the trajectory of the probe for one timeslice
     * method can be changed to adjust it for a longer period
     * @param xTitan the position of the titan one timeclise further
     * @param yTitan
     * @param zTitan
     * @param timeSlice
     */
    public void useEngine(double xTitan, double yTitan, double zTitan, double timeNeeded,double timeSlice){

        //give the probe the right acceleration
        for (int i = 0; i < realProbesList.size(); i++) {

            //difference in distance
            double xdif = xTitan - realProbesList.get(i).location.x;
            double ydif = yTitan - realProbesList.get(i).location.y;
            double zdif = zTitan - realProbesList.get(i).location.z;

            //speed necessary to get to that position of titan
            double xvel = xdif/(timeNeeded);
            double yvel = ydif/(timeNeeded);
            double zvel = zdif/(timeNeeded);
            //the amount of acceleration needed to get that velocity
            double xacc = xvel - realProbesList.get(i).velocity.x;
            double yacc = yvel - realProbesList.get(i).velocity.y;
            double zacc = zvel - realProbesList.get(i).velocity.z;
            //realProbesList.get(i).acceleration = new Vector3D(xacc/timeSlice, yacc/timeSlice, zacc/timeSlice);
            realProbesList.get(i).addAccelerationByForce(new Vector3D((xacc/timeSlice)*realProbesList.get(i).mass,(yacc/timeSlice)*realProbesList.get(i).mass,(zacc/timeSlice)*realProbesList.get(i).mass));
        }
    }



    public List getBodieslist(){
        return bodies;
    }

    public ArrayList<Probe> getProbeList() { return realProbesList;}

}
