package com.ibm.pzsdk;

import java.util.LinkedList;

public class PZBeaconData {
    public class TimedRSSI {

        private double rssi;
        private long ts;
        public TimedRSSI(double rssi) {
            super();
            this.rssi = rssi;
            this.ts = System.currentTimeMillis();
        }
        public double getRssi() {
            return rssi;
        }
        public long getTs() {
            return ts;
        }

    }
    public static final int HISTORY_WINDOW_IN_MILIS = 30*1000;
    private String uuid;
	private int minor;
	private int major;
	private double rssi;
	private LinkedList<TimedRSSI>rssiList = new LinkedList<TimedRSSI>();
	private double accuracy;
	private String proximity;
    private int state; /* Presence Zonce Protocol:  1 - Enter,  0 - Update, -1 - Exit */
	
	public PZBeaconData(String uuid, int major, int minor) {
		super();
		this.uuid = uuid;
		this.major=major;
		this.minor=minor;
	}
	public int getMinor() {
		return minor;
	}
	
	public int getMajor() {
		return major;
	}
	
	public double getRssi() {
		return rssi;
	}
	public double getAvgRssi() {
		long now = System.currentTimeMillis();
		if (rssiList.size()>0){
			double sum =0;
            int size = 0;
			for (TimedRSSI timedRSSI:rssiList){
				if (timedRSSI.getTs()>now-HISTORY_WINDOW_IN_MILIS) {
                    sum += timedRSSI.getRssi();
                    size++;
                }
			}

		if (size == 0)
            return rssi;
        else
		    return sum/size;
		}
		else return rssi;
	}
	public void setRssi(double rssi) {
		this.rssi = rssi;
		rssiList.addFirst(new TimedRSSI(rssi));
		if (rssiList.size()>10)
			rssiList.removeLast();
	}
    public String getUuid() {
        return uuid;
    }
	public double getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}
	public String getProximity() {
		return proximity;
	}
	public void setProximity(String proximity) {
		this.proximity = proximity;
	}

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + major;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PZBeaconData other = (PZBeaconData) obj;
		if (minor != other.minor)
			return false;
		return true;
	}


    @Override
    public String toString() {
        return "BeaconData{" +
                "uuid='" + uuid + '\'' +
                ", minor=" + minor +
                ", major=" + major +
                ", rssi=" + rssi +
                ", rssiList=" + rssiList +
                ", accuracy=" + accuracy +
                ", proximity='" + proximity + '\'' +
                '}';
    }
}
