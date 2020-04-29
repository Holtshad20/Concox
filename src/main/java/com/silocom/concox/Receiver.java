/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.silocom.concox;

import com.silocom.m2m.layer.physical.Connection;
import com.silocom.m2m.layer.physical.MessageListener;
import java.util.Arrays;
import java.util.Date;

/**
 *
 * @author Sergio 2
 */
public class Receiver implements MessageListener {

    Connection con;
    private byte[] imeiExpected;
    private int timeout;
    private static final int login = 0x01;
    private static final int locationData = 0x12;
    private static final int statusInformation = 0x13;
    private static final int stringInformation = 0x15;
    private static final int alarmData = 0x16;
    
    private ConcoxListener listener;

    public Receiver(Connection con, byte[] imeiExpected, int timeout) {
        this.con = con;
        this.imeiExpected = imeiExpected;
        this.timeout = timeout;
    }
    public void setListener(ConcoxListener listener) {
        this.listener = listener;
    }

    @Override
    public void receiveMessage(byte[] message) {

        int mType = message[4] & 0xFF; // fourth byte corresponds to message type

        switch (mType) {

            case login:

                byte[] imeiReceived = Arrays.copyOfRange(message, 4, message.length - 6);

                if (Arrays.equals(imeiReceived, imeiExpected)) {
                    //prepare message to accept
                    byte[] loginAccept = new byte[10];

                    loginAccept[0] = 0x78;  //start bit
                    loginAccept[1] = 0x78;  //start bit
                    loginAccept[2] = 0x05;  //message length
                    loginAccept[3] = message[3];  // protocol number   
                    loginAccept[4] = message[12]; //information serial number
                    loginAccept[5] = message[13]; //information serial number
                    byte[] crc = CRC16.getCRCITU(Arrays.copyOfRange(loginAccept, 2, loginAccept.length - 4)); //from message length to information serial number
                    loginAccept[6] = crc[2];  //error check
                    loginAccept[7] = crc[3];  //error check
                    loginAccept[8] = 0x0D;  //stop bit
                    loginAccept[9] = 0x0A;  //stop bit

                    con.sendMessage(loginAccept);

                } else {

                    //if the IMEI aren't the same, do nothing
                }

                if (message.length == 15) { //heartbeat packet
                    //parsear información que viene en el paquete

                    byte[] heartbeat = new byte[15];

                    heartbeat[0] = 0x78;  //start bit
                    heartbeat[1] = 0x78;  //start bit
                    heartbeat[2] = 0x05;  //message length
                    heartbeat[3] = message[3];  // protocol number   
                    heartbeat[4] = message[9]; //information serial number
                    heartbeat[5] = message[10]; //information serial number
                    byte[] crc = CRC16.getCRCITU(Arrays.copyOfRange(heartbeat, 2, heartbeat.length - 4)); //from message length to information serial number
                    heartbeat[6] = crc[2];  //error check
                    heartbeat[7] = crc[3];  //error check
                    heartbeat[8] = 0x0D;  //stop bit
                    heartbeat[9] = 0x0A;

                    con.sendMessage(heartbeat);
                }

                break;

            case locationData: {
                byte[] locationData = Arrays.copyOfRange(message, 4, message.length - 6); //from date to cell ID           
                ConcoxReport reports = Parser.locationDataParser(locationData);
                    if (listener != null) {
                    listener.onData(reports);
                }
            }
            break;

            case alarmData: {

                byte[] alarmData = Arrays.copyOfRange(message, 4, message.length - 6);
                ConcoxReport reports = Parser.alarmDataParser(alarmData);
                if (listener != null) {
                    listener.onData(reports);
                }
            }
            break;

        }

    }

    @Override
    public void receiveMessage(byte[] message, Connection con) {

    }

}
