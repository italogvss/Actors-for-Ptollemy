/* An actor that manages OPC-UA client connection to a server.

 build with: javac -source 1.8 -target 1.8 -cp "../../..;../../../lib/eclipse-milo/*;../../../lib/GSSFramework/*" -g -O ./OPCUAConnectionManager.java

 Check JVM version: javap -v OPCUAConnectionManager.class |findstr major

 Copyright (c) 2023 Itaipu Binacional - OP.DT/GSS

To include actors into the "Itaipu" library (no need of full build):
1) Edit file "ptolemy/configs/basicActorLibrary.xml" and include the following line:
  <input source="ptolemy/actor/lib/Itaipu.xml"/>
2) Create the "Itaipu.xml" file in the following location (if not already created):
  ptolemy/actor/lib/Itaipu.xml
3) Add the following lines inside the "<group>" tag:
  <entity name="OPCUAConnectionManager" class="ptolemy.actor.lib.OPCUAConnectionManager">
  </entity>
 
 */
package ptolemy.actor.lib;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import itaipu.gss.framework.OPCUA.AccessLevel;
import itaipu.gss.framework.OPCUA.DataType;
import itaipu.gss.framework.OPCUA.OPCUAClient;
import itaipu.gss.framework.OPCUA.OPCUAConnectOptions;
import itaipu.gss.framework.OPCUA.OPCUAConnectionException;
import itaipu.gss.framework.OPCUA.OPCUANode;
import itaipu.gss.framework.OPCUA.OPCUAValue;
import itaipu.gss.framework.exception.AppException;
import itaipu.gss.framework.log.AppLogger;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;

public class OPCUAConnectionManager extends TypedAtomicActor 
{

    private StringParameter endpoint;
    private OPCUAClient client = null;
    //opc.tcp://chi259.itaipu.int:4840
    private String sEndpointText = "opc.tcp://chi259.itaipu.int:4840";
 
 
    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */ 
    public OPCUAConnectionManager(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException 
    {
        super(container, name);
        
        AppLogger.setupConsoleHandler();
        //AppLogger.setupFileHandler("PtolemyII");
        
        endpoint = new StringParameter(this, "endpoint");
        endpoint.setExpression(sEndpointText);

        try {
            reconnect();
        } catch (Exception e) {
            System.out.println("OPCUAConnectionManager::OPCUAConnectionManager: Failed connecting client.");
            e.printStackTrace();
        }
        
    }

    /** Find a OPC-UA manager with the specified name for the specified
     *  actor.
     *  @param name OPC-UA manager name.
     *  @param actor The actor.
     *  @return An OPC-UA manager.
     *  @exception IllegalActionException If no OPC-UA manager is found.
     */
    public static OPCUAConnectionManager findOPCUAConnectionManager(String name,
            NamedObj actor) throws IllegalActionException 
    {
        NamedObj container = actor.getContainer();
        NamedObj opcManager = null;

        if(container instanceof CompositeActor){
            CompositeActor parent = (CompositeActor) container;
            opcManager = parent.getEntity(name);
            while (!(opcManager instanceof OPCUAConnectionManager)) {
                // Work recursively up the tree.
                parent = (CompositeActor) parent.getContainer();
                if (parent == null) {
                    throw new IllegalActionException(actor,
                        "Cannot find OPCUAdatabase manager named " + name);
                }
                opcManager = parent.getEntity(name);
            }
        } 
        return (OPCUAConnectionManager) opcManager;
    }


    /** React to a change in an attribute.  This method is called by
     *  a contained attribute when its value changes.  
     *  @param attribute The attribute that changed.
     *  @exception IllegalActionException If the change is not acceptable
     *   to this container (not thrown in this base class).
     */
    @Override
    public void attributeChanged(Attribute attribute) throws IllegalActionException
    {
        if (attribute == endpoint) {
            if(client == null){
                client = new OPCUAClient();
            }
            
            String sNewEndptText = endpoint.getExpression();
            if( !sNewEndptText.equals(sEndpointText) ){
                System.out.println( "OPCUAConnectionManager::attributeChanged: Old= " + sEndpointText + " New= " + sNewEndptText);
                sEndpointText = sNewEndptText;
            
            reconnect();

            }
        } else {
            super.attributeChanged(attribute);
        }
    }

     /** Find all available OPC-UA nodes in the server.
     *  @return A list of available nodes in the server.
     */   
    public List<OPCUANode> browseAllOpcNodes() 
    {
        return browseOpcNodes(AccessLevel.READ_ONLY, "*");
    }
    public List<OPCUANode> browseAllOpcNodes(String filterExpr) 
    {
        return browseOpcNodes(AccessLevel.READ_ONLY, filterExpr);
    }

     /** Find all available WRITABLE OPC-UA nodes in the server.
     *  @return A list of available nodes in the server.
     */   
    public List<OPCUANode> browseWritableOpcNodes() 
    {
        return browseOpcNodes(AccessLevel.READ_WRITE, "*");
    }
    public List<OPCUANode> browseWritableOpcNodes(String filterExpr) 
    {
        return browseOpcNodes(AccessLevel.READ_WRITE, filterExpr);
    }

     /** Find available OPC-UA nodes in the server, according to the specified access level.
     *  @return A list of available nodes in the server.
     */   
    public List<OPCUANode> browseOpcNodes(AccessLevel level, String filterExpr) 
    {
        List<OPCUANode> nodes = null;
        try {
            if(client.isConnected())
            nodes = client.getObjects(filterExpr, level);
        } catch (AppException e) {
            e.printStackTrace();
        } catch (OPCUAConnectionException e) {
            System.out.println( "OPCUAConnectionManager::browseOpcNodes: OPCUAConnectionException:");
            e.printStackTrace();
        }
        return nodes;
        
    }
 
    /** Read the specified OPC-UA node value
     *  @param sNodeName OPC-UA node name.
     *  @param DataType The node data type (analog/accumulator/status).
     *  @return The node value.
     *  @exception OPCUAConnectionException If no OPC-UA manager is found, or client is not connected.
     */ 
    public double readOpcValue(String sNodeName, DataType tType) throws AppException, OPCUAConnectionException, IllegalActionException
    {
        double dRetVal = -Double.MAX_VALUE;
        if(!client.isConnected()) {
            // Raise exception:
            throw new IllegalActionException(this, "Client is not connected to a server.");
        }
        else {
            // Read value
            OPCUANode node = new OPCUANode(sNodeName, tType);
            Map<OPCUANode, OPCUAValue> opcuaNodeOPCUAValueMap = client.readData(node);

            for (Map.Entry<OPCUANode, OPCUAValue> entry : opcuaNodeOPCUAValueMap.entrySet()) {
                OPCUAValue value = entry.getValue();
                dRetVal = value.getValue();
            }
        }
        
        return dRetVal;
    }
  
    /** Write the specified OPC-UA node value
     *  @param sNodeName OPC-UA node name.
     *  @param DataType The node data type (analog/accumulator/status).
     *  @return The node value.
     *  @exception OPCUAConnectionException If no OPC-UA manager is found, or client is not connected.
     */   
    public void writeOpcValue(String sNodeName, DataType dataType, double value) throws AppException, IllegalActionException
    {
        OPCUANode node = new OPCUANode(sNodeName, dataType);            
        OPCUAValue val = new OPCUAValue(new Date(), value, 0);
        Map<OPCUANode, OPCUAValue> writeMap = new HashMap<OPCUANode, OPCUAValue>();                       
        writeMap.put(node, val);
        try {
            client.writeData(writeMap);
        } catch (Exception e) {
            throw new IllegalActionException(this, "Client is not connected to a server.");
        }
    }
    
    public boolean isConnected()
    {
        return client.isConnected();
    }


    /** Clones the manager.
     *  @exception CloneNotSupportedException
     */
    @Override
    public OPCUAConnectionManager clone(Workspace workspace) throws CloneNotSupportedException 
    {
        // TODO: fix this method...
        OPCUAConnectionManager newObject = (OPCUAConnectionManager) super.clone(workspace);
        OPCUAConnectOptions options = new OPCUAConnectOptions();
        try {
            String sEndptText = endpoint.getExpression(); //endpoint.getToken().toString().replace("\"", "");
            
            System.out.println("OPCUAConnectionManager::clone: Endpoint text=" + sEndptText);
            
            newObject.endpoint = new StringParameter(newObject, "endpoint");
            newObject.endpoint.setExpression( sEndptText );

            options.setServerEndpoint(sEndptText);
            options.setApplicationURI(sEndptText);
        } catch (Exception e) {
            System.out.println("OPCUAConnectionManager::clone: Cannot set configuration options!");
            e.printStackTrace();
        }
        
        try {
            newObject.reconnect();
        } catch (Exception e) {
            System.out.println("OPCUAConnectionManager::clone: Connection Failed!");
            e.printStackTrace();
        }

        return newObject;
    }

    /** Closes current connection and creates new one to the endpoint.
     *  @exception IllegalActionException
     */
    public void reconnect() throws IllegalActionException
    {
        System.out.println("OPCUAConnectionManager::reconnect: Endpoint text=" + sEndpointText + " Client: " + client);
        
        if(client != null && client.isConnected()){
            try {
                client.disconnect();
		    } finally {
			    client = null;
		    }
            System.out.println("OPCUAConnectionManager::reconnect: Disconnecting client...");
        }

        client = new OPCUAClient();
        OPCUAConnectOptions options = new OPCUAConnectOptions();

        options.setServerEndpoint(sEndpointText);
        options.setApplicationURI(sEndpointText);
        try {
            System.out.println("OPCUAConnectionManager::reconnect(): Trying to reconnect to endpoint: " + sEndpointText);
            client.connect(2, options);
        } catch (Exception e) { // FIXME - return failure or throw exception...
            System.out.println("OPCUAConnectionManager::reconnect(): Failed reconnecting");
            e.printStackTrace();
        }

    }

}

