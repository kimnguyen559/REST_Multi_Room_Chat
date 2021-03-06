package services;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class ShoppingApplication extends Application
{
   private Set<Object> singletons = new HashSet<Object>();

   public ShoppingApplication()   {      
      singletons.add(new ChatResource());
   }

   @Override
   public Set<Object> getSingletons()
   {
      return singletons;
   }
}