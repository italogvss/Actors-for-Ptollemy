# Development of Actors for Ptolemy II Simulator with OPC UA Support

This repository documents the development of unique actors for the Ptolemy II simulator. These actors have the capability to fetch and send data from an OPC UA database for use during simulator execution. The project has resulted in the creation of four actors, with a notable emphasis on the "Client," an abstract class that serves as the foundation for implementing OPC UA data readers and writers.

# Overview

The focal point of this project is to expand the capabilities of the Ptolemy II simulator through the integration of OPC UA database data. The goal is to enable users to bring in relevant information and send simulation results back to the database. This provides a more realistic and interactive environment for running simulations.
Developed Actors

* OPCUAClient (Abstract Class): The abstract class "Client" is the heart of this project. It provides the core structure for implementing OPC UA data readers and writers. Inheriting from this class allows for customization of reading and writing functionalities based on user requirements.
* OPCUARead: The "Reader" actor is derived from the "Client" class. It is responsible for fetching data from the OPC UA database to be used as input during simulator execution. This enables the use of real-time information in your simulations.
* OPCUAWrite: Similar to the "Reader," the "Writer" actor also inherits from the "Client" class. Its role is to send simulation results back to the OPC UA database. This allows for post-simulation storage and analysis of the obtained results.
* OPCUAConnectionManager: The "Connection Manager" actor is a crucial component that defines endpoints and tables to be used by the readers and writers. It facilitates the configuration of communication between the "Reader" and "Writer" actors and the OPC UA database.
