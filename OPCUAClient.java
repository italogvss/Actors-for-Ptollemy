/* An actor that implements an OPC-UA client reading a value from a server.

 build with: javac -source 1.8 -target 1.8 -cp "../../..;../../../lib/eclipse-milo/*;../../../lib/GSSFramework/*" -g -O ./OPCUAClient.java

 Check JVM version: javap -v OPCUAClient.class |findstr major

 Copyright (c) 2023 Itaipu Binacional - OP.DT/GSS

 
 */
package ptolemy.actor.lib;

import java.util.List;

import itaipu.gss.framework.OPCUA.DataType;
import itaipu.gss.framework.OPCUA.OPCUANode;
import itaipu.gss.framework.log.AppLogger;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.data.DoubleToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.expr.OPCUAPointParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ptolemy.data.BooleanToken;

public class OPCUAClient extends TypedAtomicActor{

    private StringParameter managerParam;
    private Parameter node;
    private String tagName;
    private Parameter pDataType;
    private DataType dataType;
    private OPCUAConnectionManager myManager = null;

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */ 
    public OPCUAClient(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
        super(container, name);

        AppLogger.setupConsoleHandler();
        //AppLogger.setupFileHandler("PtolemyII");

        managerParam = new StringParameter(this, "OPCUAConnectionManager");
        managerParam.setExpression("OPCUAConnectionManager");
        managerParam.setStringMode(true);
        managerParam.moveToFirst();
        
        node = new OPCUAPointParameter(this, "Point");
        node.setStringMode(true);

        pDataType = new Parameter(this, "DataType");
        pDataType.setStringMode(true);
        pDataType.setExpression("Analog");
        dataType = DataType.Analog;;

        pDataType.addChoice("Analog");
        pDataType.addChoice("Status");
        pDataType.addChoice("Accumulator");
    }


    /** Populates the list of choices for the "point" parameter
     *  with the list of nodes from the OPC-UA server.
     *  @param node The Parameter instance to be updated.
     *  @return The updated Parameter instance.
     *  @exception IllegalActionException.
     */
    public Parameter setNodesTag(Parameter node, String filterExpr) throws IllegalActionException {
        String managerName = managerParam.stringValue();
        List<OPCUANode> allNodes = null;

        if(myManager != null){
            System.out.println("OPCUAClient::setNodesTag: Manager=" + managerName + " connected? " + myManager.isConnected());
            allNodes = myManager.browseAllOpcNodes(filterExpr);
        }
        if (allNodes != null) {
            node.removeAllChoices();
            for (OPCUANode opcuaNode : allNodes) {
                node.addChoice(opcuaNode.getTagName());
            }
        } else {
            System.out.println("OPCUAClient::setNodesTag: Nodes list is null...");
            //node.removeAllChoices();
        }
        //node.setExpression(node.getExpression());
        //node.valueChanged(node);
        //node.updateContent();

        return node;
    }

    /** React to a change in an attribute.  This method is called by
     *  a contained attribute when its value changes.  
     *  @param attribute The attribute that changed.
     *  @exception IllegalActionException If the change is not acceptable
     *   to this container (not thrown in this base class).
     */
    @Override
    public void attributeChanged(Attribute attribute) throws IllegalActionException {

        if (attribute == managerParam) {
            String managerName = managerParam.stringValue();
            myManager = OPCUAConnectionManager.findOPCUAConnectionManager(managerName, this);
        } else if (attribute == pDataType) {
            System.out.println("OPCUAClient::attributeChanged: Data type changed: " + attribute);
            switch (pDataType.getToken().toString().replace("\"", "")) {
              case "Analog":
                dataType = DataType.Analog;
                break;
              case "Status":
                dataType = DataType.Status;
                break;
              case "Accumulator":
                dataType = DataType.Accumulator;
                break;
              default:
                throw new IllegalActionException(this, "DataType invalid.");
            }
        } else if (attribute == node){
            tagName = node.getToken().toString().replace("\"", "");
            System.out.println("OPCUAClient::attributeChanged: Node changed: " + attribute);
        } else {
            super.attributeChanged(attribute);
        }
    }

    /** Getters:
     */
    public OPCUAConnectionManager getMyManager() {
        // FIXME - myManager can be modified outside the getter...
        return this.myManager;
    }
    
    public String getTagName() {
        return tagName;
    }

    public DataType getDataType() {
        return dataType;
    }
    
    public Parameter getNode() {
        return node;
    }

}
