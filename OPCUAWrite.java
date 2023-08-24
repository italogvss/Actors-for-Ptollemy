/* An actor that implements an OPC-UA client writing a value to a server.

 build with: javac -source 1.8 -target 1.8 -cp "../../..;../../../lib/eclipse-milo/*;../../../lib/GSSFramework/*" -g -O ./OPCUAWrite.java

 Check JVM version: javap -v OPCUAWrite.class |findstr major

 Copyright (c) 2023 Itaipu Binacional - OP.DT/GSS

To include actors into the "Itaipu" library (no need of full build):
1) Edit file "ptolemy/configs/basicActorLibrary.xml" and include the following line (if not already added):
  <input source="ptolemy/actor/lib/Itaipu.xml"/>
2) Create the "Itaipu.xml" file in the following location (if not already created):
  ptolemy/actor/lib/Itaipu.xml
3) Add the following lines inside the "<group>" tag:
  <entity name="OPCUAWrite" class="ptolemy.actor.lib.OPCUAWrite">
  </entity>
 
 */
package ptolemy.actor.lib;

import java.util.List;

import itaipu.gss.framework.OPCUA.DataType;
import itaipu.gss.framework.OPCUA.OPCUANode;
import itaipu.gss.framework.exception.AppException;
import itaipu.gss.framework.log.AppLogger;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ptolemy.data.ScalarToken;

public class OPCUAWrite extends OPCUAClient{

    private TypedIOPort input;

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public OPCUAWrite(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
        super(container, name);

        AppLogger.setupConsoleHandler();
        //AppLogger.setupFileHandler("PtolemyII");

        input = new TypedIOPort(this, "value");
        input.setTypeEquals(BaseType.DOUBLE);
        input.setInput(true);

    }


    @Override
    public OPCUAWrite clone(Workspace workspace) throws CloneNotSupportedException {
        OPCUAWrite newObject = (OPCUAWrite) super.clone(workspace);

        newObject.input.setTypeEquals(BaseType.DOUBLE);

        return newObject;
    }
    
    /** Writes the input value to the  corresponding node in the OPC-UA server.
     *  @exception IllegalActionException If the OPC-UA server query fails.
     */    
    @Override
    public void fire() throws IllegalActionException {
        super.fire();

        if (input.hasToken(0)) {
            ScalarToken in = (ScalarToken) input.get(0);
            try {
                getMyManager().writeOpcValue(getTagName(), getDataType(), in.doubleValue());
            } catch (AppException e) {
                e.printStackTrace();
                throw new IllegalActionException(this,
                    "Failed writing value to OPC-UA server. Tip: Maybe the Manager is not connected, or parameters Point/DataType are not defined.");
            }
        }
    }

}
