import ciranda.*;
import ciranda.features.*;
import ciranda.utils.*;
import ciranda.classify.*;

public class SpeechActExample{

public static void main(String[] args){

String msg = "hi jim, when are you coming back? how's the trip to Denmark going? pretty cold, huh? well, things here are going well, no problems so far. so...enjoy your trip, regards, jose"; 

SpeechAct sa = new SpeechAct();
sa.loadMessage(msg);

System.out.println("Req = "+sa.hasRequest()+"  , Confidence= "+sa.getRequestConfidence());
System.out.println("Dlv = "+sa.hasDeliver()+"  , Confidence= "+sa.getDeliverConfidence());
System.out.println("Cmt = "+sa.hasCommit()+"  , Confidence= "+sa.getCommitConfidence());
System.out.println("Prop = "+sa.hasPropose()+"  , Confidence= "+sa.getProposeConfidence());
System.out.println("Meet = "+sa.hasMeet()+"  , Confidence= "+sa.getMeetConfidence());
System.out.println("Ddata = "+sa.hasDdata()+"  , Confidence= "+sa.getDdataConfidence());

System.out.println("*");

//another message - don't need to create another SpeechAct object. Just load the new message
String msg2 = "hi Lula, you can trust me: I will vote for your party in the next elections. I will also make sure that all congressmen approves your next bill. Best regards. Severino"; 

String msg3 = "GSOC Proposal on Thursday hi machan, I will talk to you next monday";
sa.loadMessage(msg3);

System.out.println("Req = "+sa.hasRequest()+"  , Confidence= "+sa.getRequestConfidence());
System.out.println("Dlv = "+sa.hasDeliver()+"  , Confidence= "+sa.getDeliverConfidence());
System.out.println("Cmt = "+sa.hasCommit()+"  , Confidence= "+sa.getCommitConfidence());
System.out.println("Prop = "+sa.hasPropose()+"  , Confidence= "+sa.getProposeConfidence());
System.out.println("Meet = "+sa.hasMeet()+"  , Confidence= "+sa.getMeetConfidence());
System.out.println("Ddata = "+sa.hasDdata()+"  , Confidence= "+sa.getDdataConfidence());

}
}