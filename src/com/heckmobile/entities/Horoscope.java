package com.heckmobile.entities;

import java.io.Serializable;
import java.util.Date;

import android.util.Log;

public class Horoscope implements Serializable{
	
	public String sign;
	public Date date;
	public String description;
	
	public Horoscope(String sign, Date date, String description) {
		this.sign = sign;
		this.date = date;
		this.description = description;
	}
	
}


