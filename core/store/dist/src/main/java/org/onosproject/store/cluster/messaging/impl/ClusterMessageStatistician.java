package org.onosproject.store.cluster.messaging.impl;

import com.google.common.base.Charsets;
import org.onlab.netty.InternalMessage;
import org.onlab.nio.Message;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessageSubject;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by paradise on 2015/11/19.
 */
public class ClusterMessageStatistician {

    private Map<Endpoint,Map<MessageSubject,Long>> receiveCount;
    private Map<Endpoint,Map<MessageSubject,Long>> sendCount;
    private Map<Endpoint,Map<MessageSubject,Long>> receiveLength;
    private Map<Endpoint,Map<MessageSubject,Long>> sendLength;
    private NodeId  localNodeId;
    private Endpoint localEp;

    private ClusterCommunicationManager clusterCommunicationManager;

    public ClusterMessageStatistician(ClusterCommunicationManager clusterCommunicationManager){
        receiveCount = new HashMap<Endpoint,Map<MessageSubject,Long>>();
        sendCount = new HashMap<Endpoint,Map<MessageSubject,Long>>();
        receiveLength = new HashMap<Endpoint,Map<MessageSubject,Long>>();
        sendLength = new HashMap<Endpoint,Map<MessageSubject,Long>>();
        this.clusterCommunicationManager = clusterCommunicationManager;
        this.localNodeId = clusterCommunicationManager.getLocalNodeId();
        localEp = clusterCommunicationManager.getLocalEp();

    }

    public void handleSendedMessage(Endpoint ep, String type, byte[] payload){
        //sendCount++
        Map<MessageSubject,Long> mc = sendCount.get(ep);
        MessageSubject messageSubjectCount = new MessageSubject(type);
        if(null == mc){
            mc = new HashMap<MessageSubject,Long>();
            sendCount.put(ep,mc);
        }
        Long count = mc.get(messageSubjectCount);
        if(null == count){
            mc.put(messageSubjectCount,1L);
        }else{
            mc.put(messageSubjectCount,++count);
        }
        //sendLength+
        InternalMessage internalMessage = toInternalMessage(ep,type,payload);
        MessageSubject messageSubjectLength = new MessageSubject(type);
        Map<MessageSubject,Long> ml = sendLength.get(ep);
        if(null == ml){
            ml = new HashMap<MessageSubject,Long>();
            sendLength.put(ep,ml);
        }
        Long length = ml.get(messageSubjectLength);
        if(null == length){
            ml.put(messageSubjectLength,getLength(internalMessage));
        }else{
            ml.put(messageSubjectLength,length + getLength(internalMessage));
        }
    }

    public void handleReceivedMessage(ClusterMessage message){
        //receiveCount++
        NodeId sendNodeId = message.sender();
        Endpoint ep = clusterCommunicationManager.getEndPointByNodeId(sendNodeId);
        Map<MessageSubject,Long> mc = receiveCount.get(ep);
        MessageSubject messageSubjectCount = message.subject();
        if(null == mc){
            mc = new HashMap<MessageSubject,Long>();
            receiveCount.put(ep,mc);
        }
        Long count = mc.get(messageSubjectCount);
        if(null == count){
            mc.put(messageSubjectCount,1L);
        }else{
            mc.put(messageSubjectCount,++count);
        }
        //receiveLength++
        InternalMessage internalMessage = toInternalMessage(message);
        MessageSubject messageSubjectLength =  message.subject();
        Map<MessageSubject,Long> ml = receiveLength.get(ep);
        if(null == ml){
            ml = new HashMap<MessageSubject,Long>();
            receiveLength.put(ep,ml);
        }
        Long length = ml.get(messageSubjectLength);
        if(null == length){
            ml.put(messageSubjectLength,getLength(internalMessage));
        }else{
            ml.put(messageSubjectLength,length + getLength(internalMessage));
        }
    }

    public Map<Endpoint,Map<MessageSubject,Long>> getReceivedMessageCount(){
        return receiveCount;
    }

    public Map<Endpoint,Map<MessageSubject,Long>> getSendedMessageCount(){
        return sendCount;
    }

    public Map<Endpoint, Map<MessageSubject, Long>> getReceivedMessageLength() {
        return receiveLength;
    }

    public Map<Endpoint, Map<MessageSubject, Long>> getSendedMessageLength() {
        return sendLength;
    }

    /**
     * sended message translate
     * @param ep
     * @param type
     * @param payload
     * @return
     */
    private InternalMessage toInternalMessage(Endpoint ep, String type, byte[] payload){
        InternalMessage internalMessage = null;
        internalMessage = new InternalMessage(0,localEp,type,payload);
        return internalMessage;
    }

    /**
     * received message translate
     * @param message
     * @return
     */
    private InternalMessage toInternalMessage(ClusterMessage message){
        InternalMessage internalMessage = null;
        internalMessage = new InternalMessage(0,clusterCommunicationManager.getEndPointByNodeId(message.sender()),message.subject().value(),message.payload());
        return internalMessage;
    }

    private long getLength(InternalMessage message){
        long length = 0;
        if(null == message) return length;
        //messageId length
        length += Long.BYTES;
        Endpoint sender = message.sender();
        //IpAddress length
        IpAddress senderIp = sender.host();
        length += 1;
        length += senderIp.toOctets().length;

        //sender port length
        length += Integer.BYTES;

        byte[] messageTypeBytes = message.type().getBytes(Charsets.UTF_8);

        //length of message type
        length += messageTypeBytes.length;

        //message type bytes
        length += messageTypeBytes.length;

        byte[] payload = message.payload();

        //payload length
        length += Integer.BYTES;
        length += payload.length;
        return length;
    }
}
