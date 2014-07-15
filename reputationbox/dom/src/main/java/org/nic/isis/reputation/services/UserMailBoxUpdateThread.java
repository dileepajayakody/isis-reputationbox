package org.nic.isis.reputation.services;

import java.util.Date;

import javax.inject.Inject;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.nic.isis.reputation.dom.UserMailBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMailBoxUpdateThread extends Thread {

	private UserMailBox mailBox;
	private ContextIOService contextIO;

	private final static Logger logger = LoggerFactory
			.getLogger(UserMailBoxUpdateThread.class);

	public UserMailBoxUpdateThread(UserMailBox mb, ContextIOService cio) {
		this.mailBox = mb;
		this.contextIO = cio;
	}

	public void run() {
		mailBox.setSyncing(true);
		//run the thread until contextio returns no more results..
		while (mailBox.isSyncing()){
			mailBox = contextIO.updateMailBox(mailBox, 10);
		}
		logger.info("indexed all messages upto now in the mailbox : "
				+ mailBox.getEmailCount());
	}
}
