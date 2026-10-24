package app.gap.clarity;

import java.util.ArrayList;

/** Presentation history only. 1 pushes, -1 pops, 0 refreshes. */
final class NavigationState {
    private final ArrayList<String> history=new ArrayList<>();
    String current(){return history.isEmpty()?"":history.get(history.size()-1);}
    String parent(){return history.size()<2?"":history.get(history.size()-2);}
    int visit(String key){
        if(key.equals(current()))return 0;
        if(history.isEmpty()){history.add(key);return 0;}
        int previous=history.lastIndexOf(key);
        if(previous>=0){while(history.size()>previous+1)history.remove(history.size()-1);return -1;}
        // Completing onboarding returns to a fresh root instead of reopening it on Back.
        if(key.equals("home")){history.clear();history.add(key);return -1;}
        history.add(key);return 1;
    }
}
