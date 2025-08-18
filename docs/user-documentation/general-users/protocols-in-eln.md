 
Use of protocols in ELN
============

## How to use protocols in Experimental Steps and Entries

When adding protocols to an *Experimental Step*, an *Entry* or any other custom *Object* type, two options are
available:

1.  Link to an existing **Protocol** stored in the *Inventory* or in the *ELN*. This option can be used if the protocol was followed exactly in all steps as described.
2.  Create a **local copy of the Protocol** from the *Inventory* (or *ELN*) in the current *Experiment*. This should be done if some steps of the main
    protocol were modified. These modifications can be edited in the local copy of the protocol, while the main protocol is left untouched.

 

To create a local copy of an existing protocol under the current *Experiment*:

1.  Add a protocol as parent.
2.  From the **Operations** dropdown in the parents table select **Copy to Experiment.**
3.  Provide the **Object code** for the new protocol.
4.  A copy of the protocol is created under the current *Experiment*, where the user can modify it. This copy has the original protocol set as parent, so that connection between the two is clear.
 

![image info](img/copy-protocol-exp-step-1024x233.png)
