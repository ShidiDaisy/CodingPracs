// **********************************************************************
//
// Copyright (c) 2003-2013 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

import Demo.*;

public class Subscriber extends Ice.Application
{
    public class ClockI extends _ClockDisp
    {
        public void
        tick(String date, Ice.Current current)
        {
            System.out.println(date);
        }
    }
    

    public int
    run(String[] args)
    {
        args = communicator().getProperties().parseCommandLineOptions("Clock", args);

        String topicName = "time";
        String id = null;
       
        IceStorm.TopicManagerPrx manager = IceStorm.TopicManagerPrxHelper.checkedCast(
            communicator().propertyToProxy("TopicManager.Proxy"));
        if(manager == null)
        {
            System.err.println("invalid proxy");
            return 1;
        }

        //
        // Retrieve the topic.
        //
        IceStorm.TopicPrx topic;
        try
        {
            topic = manager.retrieve(topicName);
        }
        catch(IceStorm.NoSuchTopic e)
        {
            try
            {
                topic = manager.create(topicName);
            }
            catch(IceStorm.TopicExists ex)
            {
                System.err.println(appName() + ": temporary failure, try again.");
                return 1;
            }
        }

        Ice.ObjectAdapter adapter = communicator().createObjectAdapter("Clock.Subscriber");

        //
        // Add a servant for the Ice object. If --id is used the
        // identity comes from the command line, otherwise a UUID is
        // used.
        //
        // id is not directly altered since it is used below to detect
        // whether subscribeAndGetPublisher can raise
        // AlreadySubscribed.
        //
        Ice.Identity subId = new Ice.Identity(id, "");
        if(subId.name == null)
        {
            subId.name = java.util.UUID.randomUUID().toString();
        }
        Ice.ObjectPrx subscriber = adapter.add(new ClockI(), subId);

        //
        // Activate the object adapter before subscribing.
        //
        adapter.activate();

        java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
        //
        // Set up the proxy.
        //
 
        
        try
        {
            topic.subscribeAndGetPublisher(qos, subscriber);
        }
        catch(IceStorm.AlreadySubscribed e)
        {
            e.printStackTrace();
			return 1;
        }
        catch(IceStorm.BadQoS e)
        {
            e.printStackTrace();
            return 1;
        }

        shutdownOnInterrupt();
        communicator().waitForShutdown();

        topic.unsubscribe(subscriber);

        return 0;
    }

    public static void
    main(String[] args)
    {
        Subscriber app = new Subscriber();
        int status = app.main("Subscriber", args, "config.sub");
        System.exit(status);
    }
}
