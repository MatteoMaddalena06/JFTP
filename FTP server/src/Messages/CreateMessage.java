/*
    Class used to share the createMessage method 
    between the ErrorMessage, LogMessage, and ResponseMessage classes
*/
public class CreateMessage
{
    private String[] messages;
    private final String charToReplaceRegex = "((?=\\*))";

    public CreateMessage(String[] messages)
    { this.messages = messages; }

    public String createMessage(int code, String[] strings)
    {
        String[] messageTokens= messages[code].split(charToReplaceRegex);
        String finalMessage = messageTokens[0];

        if(strings.length != messageTokens.length - 1)
          return "Unable to create message.";

        for(int i = 1; i < messageTokens.length; i++)
          finalMessage += messageTokens[i].replace("*", strings[i-1]);

        return finalMessage;
    }
}
