/*
 * Copyright (c) 2015 http://www.adho.org/
 * License: see LICENSE file
 */
package org.adho.dhconvalidator.ui;

import java.util.Locale;

import org.adho.dhconvalidator.Messages.LocaleProvider;

import com.vaadin.server.VaadinSession;

/**
 * Provides locale from {@link VaadinSession}.
 * 
 * @author marco.petris@web.de
 *
 */
public enum VaadinSessionLocaleProvider implements LocaleProvider {

	INSTANCE,
	;
	
	@Override
	public Locale getLocale() {
		VaadinSession session = VaadinSession.getCurrent();
		
		if (session != null) {
			return session.getLocale();
		}
		
		return null;
	}
}
