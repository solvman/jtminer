/*
 * jtminer Java mining software for the Thought Network
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
 * 
 * Based on code from Litecoin JMiner
 * Copyright 2011  LitecoinPool.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as 
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package live.thought.jtminer;

public enum Notification
{
  SYSTEM_ERROR,
  PERMISSION_ERROR,
  CONNECTION_ERROR,
  AUTHENTICATION_ERROR,
  COMMUNICATION_ERROR,
  LONG_POLLING_FAILED,
  LONG_POLLING_ENABLED,
  NEW_BLOCK_DETECTED,
  NEW_WORK,
  POW_TRUE,
  POW_FALSE,
  TERMINATED
}
