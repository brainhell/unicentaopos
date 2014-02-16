//    uniCenta oPOS  - Touch Friendly Point Of Sale
//    Copyright (c) 2009-2013 uniCenta & previous Openbravo POS works
//    http://www.unicenta.com
//
//    This file is part of uniCenta oPOS
//
//    uniCenta oPOS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//   uniCenta oPOS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with uniCenta oPOS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.scale;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.ticket.ProductInfoExt;
import gnu.io.*;
import java.io.*;
import java.util.TooManyListenersException;
import java.awt.Component;
import javax.swing.ImageIcon;


/**
 *
 * @author  De Kruidenier
 */
public class ScaleANDEKi implements Scale, SerialPortEventListener {
    
    private Component m_parent;
    
    private CommPortIdentifier m_PortIdPrinter;
    private SerialPort m_CommPortPrinter;  
    private String m_sPortScale;
    
    private OutputStream m_out;
    private InputStream m_in;

    private static final int SCALE_READY = 0;
    private static final int SCALE_READING = 1;
    private static final int SCALE_READINGDECIMALS = 2;
 
    private String m_sWeight;
    private Double m_dWeight;
    private Double m_dWeightBuffer;
    private Double m_dWeightDecimals;
    private int m_iStatusScale;
    
    private ProductInfoExt m_product;
      
    /** Creates a new instance of Scale AND EKi Series */
    public ScaleANDEKi(String portPrinter, Component parent) {
        
        m_parent = parent;
      
        m_sPortScale = portPrinter;
        
        m_out = null;
        m_in = null;
        
        m_iStatusScale = SCALE_READY; 
        m_sWeight = "";
        m_dWeight = 0.0;
        m_dWeightBuffer = 0.0;
        m_dWeightDecimals = 1.0;
        
                
    }
    
    @Override
    public Double readWeight() {
        return readWeight(null);
    }
        
    @Override
    public Double readWeight(ProductInfoExt prod) {
        
        synchronized(this) {

            if (m_iStatusScale != SCALE_READY) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
                if (m_iStatusScale != SCALE_READY) {
                    m_iStatusScale = SCALE_READY;
                }
            }
            
            
            m_sWeight = "";
            m_dWeight = 0.0;
            m_dWeightBuffer = 0.0;
            m_dWeightDecimals = 1.0;
                   
            write(new byte[] {0x51, 0x0D, 0x0A}); // Q + CRLF - AND EK-610i series
            flush();             
            
            try {
                wait(1000);
            } catch (InterruptedException e) {
            }
        }
        
        
        m_dWeight = JDialogScale.showEditNumber(m_parent, AppLocal.getIntString("label.scale"), 
                        AppLocal.getIntString("label.scaleinput"), 
                        new ImageIcon(ScaleANDEKi.class.getResource("/com/openbravo/images/ark2.png")),
                        prod);
        
        return m_dWeight;
    }
    
    private void flush() {
        try {
            m_out.flush();
        } catch (IOException e) {
        }        
    }
    
    private void write(byte[] data) {
        try {  
            if (m_out == null) {
                m_PortIdPrinter = CommPortIdentifier.getPortIdentifier(m_sPortScale);                  
                m_CommPortPrinter = (SerialPort) m_PortIdPrinter.open("PORTID", 2000);       

                m_out = m_CommPortPrinter.getOutputStream(); // Tomamos el chorro de escritura   
                m_in = m_CommPortPrinter.getInputStream();
                
                m_CommPortPrinter.addEventListener(this);
                m_CommPortPrinter.notifyOnDataAvailable(true);
                
                m_CommPortPrinter.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE); 
            }
            m_out.write(data);
        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | TooManyListenersException | IOException e) {
        }        
    }
    
    @Override
    public void serialEvent(SerialPortEvent e) {

        switch (e.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                try {
                    while (m_in.available() > 0) {
                        int b = m_in.read();

                        m_sWeight += (char)b;
                                                                      
                        if (b == 0x000A) { // LF
                            synchronized (this) {
                                // Check if scaling units is in gram
                                String scaleLable = m_sWeight.contains("g") ? "label.scaleinput" : "label.scalewrongunit";

                                JDialogScale.setTitle(AppLocal.getIntString("label.scale"), 
                                    AppLocal.getIntString(scaleLable), 
                                    new ImageIcon(ScaleANDEKi.class.getResource("/com/openbravo/images/ark2.png")));

                                m_iStatusScale = SCALE_READY;
                                m_dWeight = m_dWeightBuffer / m_dWeightDecimals;
                                JDialogScale.setNumber(m_dWeight);
                                notifyAll();
                                m_sWeight = "";
                                m_dWeight = 0.0;
                                m_dWeightBuffer = 0.0; 
                                m_dWeightDecimals = 1.0;
                            }
                        } else if ((b > 0x002F && b < 0x003A) || b == 0x002E){
                            // Get ONLY decimal values from scaling device 
                            synchronized(this) {
                                if (m_iStatusScale == SCALE_READY) {
                                    m_sWeight = "";
                                    m_dWeight = 0.0;
                                    m_dWeightBuffer = 0.0; 
                                    m_dWeightDecimals = 1.0;
                                    m_iStatusScale = SCALE_READING;
                                }
                                if (b == 0x002E) {
                                    m_iStatusScale = SCALE_READINGDECIMALS;
                                } else {
                                    m_dWeightBuffer = m_dWeightBuffer * 10.0 + b - 0x0030;
                                    if (m_iStatusScale == SCALE_READINGDECIMALS) {
                                        m_dWeightDecimals *= 10.0;
                                    }
                                }
                           }
                        }
                    }
                } catch (IOException eIO) {}
                break;
        }
    }       
}
