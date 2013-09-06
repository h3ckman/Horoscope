package com.heckmobile.entities;

import java.io.Serializable;
import java.util.Date;

import android.util.Log;

public class Horoscope implements Serializable{
	
	public int id;
	public String sign;
	public Date date;
	public String description;
	
	public Horoscope(int id, String sign, Date date, String description) {
		this.id = id;
		this.sign = sign;
		this.date = date;
		this.description = description;
	}
	
}


