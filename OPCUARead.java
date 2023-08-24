/* An actor that implements an OPC-UA client reading a value from a server.

 build with: javac -source 1.8 -target 1.8 -cp "../../..;../../../lib/eclipse-milo/*;../../../lib/GSSFramework/*" -g -O ./OPCUARead.java

 Check JVM version: javap -v OPCUARead.class |findstr major

 Copyright (c) 2023 Itaipu Binacional - OP.DT/GSS

To include actors into the "Itaipu" library (no need of full build):
1) Edit file "ptolemy/configs/basicActorLibrary.xml" and include the following line (if not already added):
  <input source="ptolemy/actor/lib/Itaipu.xml"/>
2) Create the "Itaipu.xml" file in the following location (if not already created):
  ptolemy/actor/lib/Itaipu.xml
3) Add the following lines inside the "<group>" tag:
  <entity name="OPCUARead" class="ptolemy.actor.lib.OPCUARead">
  </entity>
 
 */
package ptolemy.actor.lib;

import java.util.List;

import itaipu.gss.framework.OPCUA.DataType;
import itaipu.gss.framework.OPCUA.OPCUANode;
import itaipu.gss.framework.log.AppLogger;
import ptolemy.data.DoubleToken;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.expr.OPCUAPointParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

public class OPCUARead extends OPCUAClient{

    public TypedIOPort output = null;

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */ 
    public OPCUARead(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
        super(container, name);
        output = new TypedIOPort(this, "output", false, true);
        output.setTypeEquals(BaseType.DOUBLE);
    }

    @Override
    public OPCUARead clone(Workspace workspace) throws CloneNotSupportedException {
        OPCUARead newObject = (OPCUARead) super.clone(workspace);
        output.setTypeEquals(BaseType.DOUBLE);
        return newObject;
    }

    /** Reads the corresponding node from OPC-UA server and produce the result
     *  on the output port.
     *  @exception IllegalActionException If the OPC-UA server query fails.
     */
    @Override
    public void fire() throws IllegalActionException {
        super.fire();

        try {
            output.send(0, new DoubleToken(getMyManager().readOpcValue(getTagName(), getDataType())));
        } catch (Exception e) {
            System.out.println("OPCUARead::fire() caught exception: " + e + " -- Manager: " + getMyManager());
            e.printStackTrace();
            throw new IllegalActionException(this,
                    "Failed obtaining value from OPC-UA server. Tip: Maybe the Manager is not connected, or parameters Point/DataType are not defined.");
        }
    }

}
