/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2011 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2013 LSC Project
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */package org.lsc.utils;

import java.awt.event.ActionEvent;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.SourceProvider;
import org.mozilla.javascript.tools.debugger.SwingGui;
import org.mozilla.javascript.tools.shell.Global;

public class RhinoDebugger implements Runnable {

    private Dim            dim;
    private SwingGui       debugGui;

     private Context        cx;
    private Scriptable     scope;
    private Script         script;

    private static Global  global;

    public RhinoDebugger(String source, ContextFactory factory) {

        dim = new Dim();
        dim.setSourceProvider(new LscSourceProvider(source));
        dim.attachTo(factory);// org.mozilla.javascript.tools.shell.Main.shellContextFactory);

        debugGui = new LscRhinoGui(dim, "Debugging LSC javascript", this);
        debugGui.setExitAction(this);
        debugGui.pack();
        debugGui.setSize(600, 460);

    }

    public void initContext(Context cx, Scriptable scope, Script script) {
        this.cx = cx;
        this.scope = scope;
        this.script = script;

        if (global == null || !global.isInitialized()) {
            global = new Global(cx);
            global.setIn(System.in);
            global.setOut(System.out);
            global.setErr(System.err);
        }
        // dim.setScopeProvider(new LscScopeProvider(global));

    }

    public Object exec() {
   		debugGui.setVisible(true);
        dim.setBreak();
        dim.setBreakOnEnter(true);
        dim.setBreakOnExceptions(true);
        return script.exec(cx, scope);
    }
    
    public void execInclude(Script include) {
    	include.exec(cx, scope);
    }

	/**
     * Exit action.
     */
    @Override
    public void run() {
        debugGui.dispose();
    }

    /**
     * Is Debugger visible?
     * 
     * @return
     */
    public boolean isVisible() {
        return debugGui != null && debugGui.isVisible();
    }

    private static class LscRhinoGui extends SwingGui {

        private static final long serialVersionUID = -6368924684609097200L;
        private RhinoDebugger     debugger;

        public LscRhinoGui(Dim dim, String title, RhinoDebugger debugger) {
            super(dim, title);
            this.debugger = debugger;
        }

        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("Exit")) {
                debugger.run();
            } else {
                super.actionPerformed(e);
            }
        }
    }

//    private static class LscScopeProvider implements ScopeProvider {
//
//        /** the scope object */
//        private Scriptable scope;
//
//        /**
//         * Creates a new LscScopeProvider.
//         */
//        public LscScopeProvider(Scriptable scope) {
//            this.scope = scope;
//        }
//
//        /**
//         * Returns the scope
//         */
//        @Override
//        public Scriptable getScope() {
//            return scope;
//        }
//    }

    private static class LscSourceProvider implements SourceProvider {

        private String source;

        public LscSourceProvider(String source) {
            this.source = source;
        }

        @Override
        public String getSource(DebuggableScript script) {
            return source;
        }

    }
}
