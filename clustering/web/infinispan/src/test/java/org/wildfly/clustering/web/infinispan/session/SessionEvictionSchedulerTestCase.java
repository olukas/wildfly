/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.wildfly.clustering.web.infinispan.session;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.Evictor;
import org.wildfly.clustering.web.session.ImmutableSession;

public class SessionEvictionSchedulerTestCase {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void test() throws Exception {
        String name = "cache";
        String evictedSessionId = "evicted";
        String activeSessionId = "active";
        ImmutableSession evictedSession = mock(ImmutableSession.class);
        ImmutableSession activeSession = mock(ImmutableSession.class);
        CommandDispatcherFactory dispatcherFactory = mock(CommandDispatcherFactory.class);
        CommandDispatcher<SessionEvictionContext> dispatcher = mock(CommandDispatcher.class);
        Evictor<String> evictor = mock(Evictor.class);
        ArgumentCaptor<Command> capturedCommand = ArgumentCaptor.forClass(Command.class);
        ArgumentCaptor<SessionEvictionContext> capturedContext = ArgumentCaptor.forClass(SessionEvictionContext.class);

        when(dispatcherFactory.createCommandDispatcher(same(name), capturedContext.capture())).thenReturn(dispatcher);

        try (Scheduler scheduler = new SessionEvictionScheduler(name, evictor, dispatcherFactory, 1)) {
            SessionEvictionContext context = capturedContext.getValue();
            
            assertSame(scheduler, context);
            
            when(evictedSession.getId()).thenReturn(evictedSessionId);
            when(activeSession.getId()).thenReturn(activeSessionId);
            
            scheduler.schedule(evictedSession);

            verifyZeroInteractions(dispatcher);

            scheduler.schedule(activeSession);

            verify(dispatcher).submitOnCluster(capturedCommand.capture());
            
            capturedCommand.getValue().execute(context);
            
            verify(evictor).evict(evictedSessionId);
            verify(evictor, never()).evict(activeSessionId);
        }

        verify(dispatcher).close();
    }
}
