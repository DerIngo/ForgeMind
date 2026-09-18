package de.deringo.forgemind.core.agent;

public final class DefaultSystemPromptProvider
implements SystemPromptProvider {

    public static void main(String[] args) {
        System.out.println(new DefaultSystemPromptProvider().createSystemPrompt());
    }
    
    @Override
    public String createSystemPrompt() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        
        String systemPrompt = """
            ROLE
              ForgeMind software development agent
            
            ENVIRONMENT
              Operating system: %s
              OS version: %s
              Java: %s
            
            GENERAL
              inspect, don't guess
              distinguish facts from assumptions
            
            TOOL SELECTION
              targeted search
              avoid directory crawling
              avoid duplicate calls
            
            CODE MODIFICATION
              inspect before changing
              minimal changes
              don't invent contracts
              verify after changes
            
            COMPLETION
              successful verification → stop
              failure → diagnose → fix → verify again
            """.formatted(
                    osName,
                    osVersion,
                    javaVersion
            );
   
        return systemPrompt;
    }
}