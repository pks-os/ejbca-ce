/*
 * UsersView.java
 *
 * Created on den 18 april 2002, 23:00
 */

package se.anatom.ejbca.webdist.rainterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import se.anatom.ejbca.ra.UserAdminData;
/**
 * A class representing a set of users w
 * @author  philip
 */
public class UsersView {
        
    /** Creates a new instance of UsersView */
    public UsersView() {
      users = new ArrayList();
      sortby = new SortBy();
    }
    
    public UsersView(UserAdminData importuser, HashMap caidtonamemap){
      users = new ArrayList();
      sortby = new SortBy();        
      users.add(new UserView(importuser, caidtonamemap)); 
      
      Collections.sort(users); 
    }
    
    public UsersView(Collection importusers, HashMap caidtonamemap){ 
      users = new ArrayList();
      sortby = new SortBy();
      
      setUsers(importusers, caidtonamemap);
    }
    // Public methods.
    
    public void sortBy(int sortby, int sortorder) {
      this.sortby.setSortBy(sortby);
      this.sortby.setSortOrder(sortorder);
      
      Collections.sort(users);
    }
    
    public UserView[] getUsers(int index, int size) {       
      int endindex;  
      UserView[] returnval;
   
      if(index > users.size()) index = users.size()-1;
      if(index < 0) index =0;
      
      endindex = index + size;
      if(endindex > users.size()) endindex = users.size();
      
      returnval = new UserView[endindex-index];  
      
      int end = endindex - index;
      for(int i = 0; i < end; i++){
        returnval[i] = (UserView) users.get(index+i);   
      }
      
      return returnval;
    }
    
    public void setUsers(UserView[] users) {
      this.users.clear();
      if(users !=null && users.length > 0){       
        for(int i=0; i < users.length; i++){
          users[i].setSortBy(this.sortby);
          this.users.add(users[i]);
        }
      }
      Collections.sort(this.users);
    }
    
    public void setUsers(UserAdminData[] users, HashMap caidtonamemap) {
      UserView user;  
      this.users.clear();
      if(users !=null && users.length > 0){ 
        for(int i=0; i< users.length; i++){
          user = new UserView(users[i], caidtonamemap); 
          user.setSortBy(this.sortby);
          this.users.add(user);
        }
        Collections.sort(this.users);
      }
    }

    public void setUsers(Collection importusers, HashMap caidtonamemap) { 
        
      UserView user;  
      Iterator i;  
      this.users.clear();
      if(importusers!=null && importusers.size() > 0){
        i=importusers.iterator();
        while(i.hasNext()){
          UserAdminData nextuser = (UserAdminData) i.next();  
          user = new UserView(nextuser, caidtonamemap); 
          user.setSortBy(this.sortby);
          users.add(user);
        }
        Collections.sort(users);
      }
    }

    public void addUser(UserView user) {
       user.setSortBy(this.sortby);        
       users.add(user);
    }
    
    public int size(){
      return users.size();   
    }
    
    public void clear(){
      this.users.clear();   
    }
    // Private fields
    private ArrayList users;
    private SortBy sortby;
    
}
