package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.content.DialogInterface;
import android.support.annotation.MainThread;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by omsi on 07.07.16.
 */
public class devices {

    public ArrayList<device> dev_List;
    public ArrayList<String> dev_names;





    public class device{
        String device_name;
        String MAC;
        float azimuth;
        public device()
        {
            device_name="";
            MAC="";
            azimuth=0f;

        }

        public device(String a, String b, Float c)
        {
            device_name=a;
            MAC=b;
            azimuth=c;

        }

    }

    public devices()
    {
        dev_List=new ArrayList<device>();
        dev_names=new ArrayList<String>();

    }
    public void updateList(String name,String mac, String angle)
    {
        device dv=new device(name,mac,Float.parseFloat(angle));
        int index=dev_name_exists(dv);
        if(index<0) {
            Log.d("service","New device added");
            dev_List.add(dv);
            dev_names.add(name);
        }
        else {
            dev_List.remove(index);
            dev_List.add(dv);
        }

    }
     public int dev_name_exists(device d)
    {
        for(int i=0;i<dev_List.size();i++)
        {
            if(dev_List.get(i).device_name.contains(d.device_name) || dev_List.get(i).MAC.contains(d.MAC))
                return i;
        }
        return -1;
    }
    public device select_best()
    {
        device ret=null;
        float v1,v2;
        float orientation=MainActivity.mService.Get_orientation();
        for(int i=0;i<dev_List.size();i++)
        {
            if(i==0)
            {ret=dev_List.get(i);}
        /*    if(-90f> dev_List.get(i).azimuth|| dev_List.get(i).azimuth>90f)
                v1=Math.abs(orientation)+ ( 180-Math.abs(dev_List.get(i).azimuth));
            else
                v1=Math.abs(orientation)+ Math.abs(dev_List.get(i).azimuth);
            if(-90f> ret.azimuth || ret.azimuth>90f)
                v2=Math.abs(orientation)+ ( 180-Math.abs(ret.azimuth));
            else
                v2=Math.abs(orientation)+ Math.abs(ret.azimuth);
            if(v1<v2)
          // if(Math.abs( Math.abs(MainActivity.mService.Get_orientation() ) - ( 180-Math.abs( dev_List.get(i).azimuth )))<Math.abs( Math.abs(MainActivity.mService.Get_orientation() ) - ( 180-Math.abs( ret.azimuth ))))
          //  if(Math.abs(180-(Math.abs(MainActivity.mService.Get_orientation())+Math.abs(dev_List.get(i).azimuth)))<Math.abs(180-(Math.abs(MainActivity.mService.Get_orientation())+Math.abs(ret.azimuth))))
             */
            v1=Math.abs( 180f- (Math.abs(orientation)+Math.abs(dev_List.get(i).azimuth)));
            v2=Math.abs( 180f- (Math.abs(orientation)+Math.abs(ret.azimuth)));
            Log.d("VAL","Orient:"+MainActivity.mService.Get_orientation()+": Az:"+dev_List.get(i).azimuth+"v1:"+v1+" v2:"+v2+" :"+dev_List.get(i).device_name)  ;

            if( v1 <=  v2 )
            {
                    Log.d("VAL","ret"+ret.device_name);
                ret=dev_List.get(i);
            }

        }
        return ret;
       // Log.d("Val",MainActivity.mService.Get_orientation()+"")  ;

    }
    public void clear()
    {
        dev_List.clear();
        dev_names.clear();
        MainActivity.mService.Groupinfo=null;

    }
}
