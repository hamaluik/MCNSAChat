package com.hamaluik.MCNSAChat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

public class ChatPlayer implements Serializable {	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8520236614779881160L;
	
	/**
	* @serial
	*/
	String channel = "G";
	
	/**
	* @serial
	*/
	boolean locked = false;
	
	/**
	* @serial
	*/
	boolean poofed = false;
	
	/**
	* @serial
	*/
	boolean seeall = false;
	
	/**
	* @serial
	*/
	boolean onTimeout = false;
	
	/**
	* @serial
	*/
	long timeoutBegin = -1;
	
	/**
	* @serial
	*/
	long timeoutLength = -1;
	
	/**
	* @serial
	*/
	long timeoutLeft = -1;
	
	/**
	* @serial
	*/
	HashSet<String> listeningChannels = new HashSet<String>();
	
	/**
	* @serial
	*/
	HashSet<String> mutedPlayers = new HashSet<String>();

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		//always perform the default de-serialization first
		aInputStream.defaultReadObject();
	}
	
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		//perform the default serialization for all non-transient, non-static fields
		aOutputStream.defaultWriteObject();
	}
}
