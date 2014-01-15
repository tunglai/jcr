/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.ext.replication.transport;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 15.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: PacketListener.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public interface PacketListener
{

   /**
    * receive. Will be called this method when receive the Packet.
    * 
    * @param packet
    *          the Packet object.
    * @param sourceAddress
    *          MemberAddress, the source address 
    */
   void receive(AbstractPacket packet, MemberAddress sourceAddress);

   /**
    * onError.
    * 
    * @param sourceAddress
    *          MemberAddress, the source address
    * 
    */
   void onError(MemberAddress sourceAddress);
}