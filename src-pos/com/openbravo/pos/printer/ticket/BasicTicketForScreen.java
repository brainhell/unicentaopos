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


package com.openbravo.pos.printer.ticket;

import java.awt.Font;
import java.awt.geom.AffineTransform;

public class BasicTicketForScreen extends BasicTicket {

    private static Font BASEFONT = new Font("Monospaced", Font.PLAIN, 12).deriveFont(AffineTransform.getScaleInstance(1.0, 1.40));
    private static int FONTHEIGHT = 20;
    private static double IMAGE_SCALE = 1.0;

    @Override
    protected Font getBaseFont() {
        return BASEFONT;
    }

    @Override
    protected int getFontHeight() {
        return FONTHEIGHT;
    }

    @Override
    protected double getImageScale() {
        return IMAGE_SCALE;
    }
}