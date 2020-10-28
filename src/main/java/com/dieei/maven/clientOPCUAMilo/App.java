package com.dieei.maven.clientOPCUAMilo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.AddressSpace.BrowseOptions;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription.ChangeListener;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.BuiltinReferenceType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class App 
{
	
	static InputStreamReader reader = new InputStreamReader(System.in);
	static BufferedReader in = new BufferedReader(reader);
	
	private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
	 
	private static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false; 
	    }
	    return pattern.matcher(strNum).matches();
	}
	
	public static void main( String[] args ) throws Exception
    {
		String publicHostname = InetAddress.getLocalHost().getHostName();	
		String url = "opc.tcp://" + publicHostname + ":51210/UA/SampleServer";
		
		int ans, ns, idNum;
		String identifier;
		NodeId nodeId;
		
		List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(url).get();
		EndpointDescription[] endpointsArray = endpoints.toArray(new EndpointDescription[0]);
		
		System.out.println("Eseguo DiscoveryEndpoint sul Server " + url);
		System.out.println("Il numero di endpoint e' " + endpointsArray.length);
		
		for (int nc = 0; nc < endpointsArray.length; nc++) {
			System.out.println(nc + ")\tSecurity Mode:" + endpointsArray[nc].getSecurityMode());
			System.out.println("\tSecurity Policy " + endpointsArray[nc].getSecurityPolicyUri());
			System.out.println("\tTransport Protocol " + endpointsArray[nc].getTransportProfileUri());
			System.out.println("\tSecurity Level  " + endpointsArray[nc].getSecurityLevel());
			System.out.println("------------------------------------------------------------------------------------");
		}
		
		System.out.println("=====================================================================================");
		
		System.out.println("Quale endpoint scegli  ? ");
		int x;
		x = Integer.parseInt(in.readLine());
		EndpointDescription selectedEndpoint = endpointsArray[x];
		
		/* Codice relativo all creazione dell'oggetto Client OPC UA */
		
		//Creo una cartella temporanea "security"
    	Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }
        
        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);
    	
    	OpcUaClientConfig config = new OpcUaClientConfigBuilder()
    			.setApplicationName(LocalizedText.english("Java Client"))
    			.setApplicationUri("urn:JavaClient")
    			.setCertificate(loader.getClientCertificate())
    			.setKeyPair(loader.getClientKeyPair())
    			.setEndpoint(selectedEndpoint)
    			.build();
    	
    	OpcUaClient client = OpcUaClient.create(config);
		
		// Esiste anche questa funzione per fare tutto quanto fatto fin ora in un colpo solo.
		// Fa il discovery degli endpoints, ti permette di selezionare quale e di configurare il client.
		// OpcUaClient client = OpcUaClient.create(endpointUrl, endpoints -> endpoints ..., configBuilder -> configBuilder ...)
    	
		client.connect().get();
		
		System.out.println("\nConnesso al Server!");
		
		////////// BROWSE /////////
		//Esistono 3 modi diversi di fare il browsing su Milo:
		//1) metodo browse() e browseNodes() di un oggetto AddressSpace
		//2) metodo browse() e browseNodes() di un oggetto UaNode
		//3) metodo browse() di un oggetto OpcUaClient (low-level API, cioè più simile alla signature del servizio Browse di OPC UA)

		System.out.println("Vuoi Fare il Browsing  (y/n) ?");
		String r;
		r = in.readLine();
		if (r.matches("y")) {
			List<? extends UaNode> nodes = new LinkedList<UaNode>();
			AddressSpace addressSpace = client.getAddressSpace();
			BrowseOptions browseOptions;
			
			do {
				System.out.println("SELEZIONA LA RADICE DA CUI INIZIARE L'ESPLORAZIONE\n");
				System.out.println("1)RootFolder\n");
				System.out.println("2)ObjectsFolder\n");
				System.out.println("3)ViewsFolder\n");
				System.out.println("4)TypesFolder\n");
				System.out.println("5)Server\n");
				System.out.println("6)NodeID from Keyboard\n");
				
				ans = Integer.parseInt(in.readLine());
				
				switch (ans) {
				case 1:
					UaNode rootNode = addressSpace.getNode(Identifiers.RootFolder);
					nodes = addressSpace.browseNodes(rootNode);     // <-- browsing usando l'oggetto AddressSpoace
					break;
				case 2:
					browseOptions = BrowseOptions.builder()
													.setBrowseDirection(BrowseDirection.Forward)
													.setReferenceType(BuiltinReferenceType.HierarchicalReferences)
													.build();
					
					UaNode objectsNode = addressSpace.getNode(Identifiers.ObjectsFolder);
					nodes = addressSpace.browseNodes(objectsNode, browseOptions);
					break;
				case 3:
					browseOptions = BrowseOptions.builder()
							.setBrowseDirection(BrowseDirection.Forward)
							.setReferenceType(BuiltinReferenceType.HierarchicalReferences)
							.build();
					
					UaNode viewsObject = addressSpace.getNode(Identifiers.ViewsFolder);
					nodes = viewsObject.browseNodes(browseOptions);     // <-- browsing usando l'oggetto UaNode
					
					break;
				case 4:
					UaNode typesNode = addressSpace.getNode(Identifiers.TypesFolder);
					nodes = addressSpace.browseNodes(typesNode);
					break;
				case 5:
					UaNode serverNode = addressSpace.getNode(Identifiers.Server);
					nodes = addressSpace.browseNodes(serverNode);
					break;
				case 6:
					System.out.println("Inserisci namespace  = ");
					ns = Integer.parseInt(in.readLine());
					System.out.println("Inserisci nodeId  = ");
					identifier = in.readLine();
					if(isNumeric(identifier)) {
						idNum = Integer.parseInt(identifier);
						nodeId = new NodeId(ns, idNum);
					} else {
						nodeId = new NodeId(ns, identifier);
					}
					/* Browse con low-level API. Commentato perchè ritorna al più ReferenceDescription[] che non possono essere stampate sotto nel for() */
//					BrowseDescription browseDescription;
//					BrowseResult browseResult;
//					browseDescription = new BrowseDescription(
//								nodeId,
//								BrowseDirection.Forward,
//								Identifiers.HierarchicalReferences,
//								true,
//								Unsigned.uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue() | NodeClass.Method.getValue()),
//								Unsigned.uint(BrowseResultMask.All.getValue())
//							);
//					browseResult = client.browse(browseDescription).get();
					nodes = addressSpace.browseNodes(nodeId);
				}
				
				System.out.println("Numero Totale di risultati =  " + nodes.size());
				
				for(UaNode node:nodes) {
					System.out.println("+-- NodeId: " + node.getNodeId().toParseableString() + "       BrowseName: " + node.getBrowseName().getName() + "        NodeClass: " + node.getNodeClass());
				}
				
				System.out.println("Vuoi Continuare a fare il Browsing (y/n) ?");
				r = in.readLine();
				
			} while (r.equalsIgnoreCase("y"));
		}
		
		///////////// LETTURA VARIABILI //////////////
		/* La Read si può fare sfruttando sia una API ad alto livello che una a basso livello.
		 * Noi vedremo quella ad alto livello. */
		
		System.out.println("Vuoi Fare la Lettura delle Variabili  (y/n) ?");
		r = in.readLine();
		if (r.matches("y")) {
			AddressSpace addressSpace = client.getAddressSpace();
			DataValue value;
			String browseName;
			NodeId dataTypeId;
			do {
				// Variabili da Leggere 2,10849 (dynamic Int32) e 2,10227
				System.out.println("Lettura Valori di Variabili  ");
				System.out.println("Inserisci namespace  = ");
				ns = Integer.parseInt(in.readLine());
				System.out.println("Inserisci nodeId  = ");
				identifier = in.readLine();
				if(isNumeric(identifier)) {
					idNum = Integer.parseInt(identifier);
					nodeId = new NodeId(ns, idNum);
				} else {
					nodeId = new NodeId(ns, identifier);
				}
				
				UaNode varNode = addressSpace.getNode(nodeId);
				if(varNode instanceof UaVariableNode) {
					value = ((UaVariableNode) varNode).readValue();  //Da notare la differenza con getValue()! getValue() non legge live dal Server!
					browseName = varNode.readBrowseName().getName();
					dataTypeId = ((UaVariableNode) varNode).getDataType();
					System.out.println("=============================================");
					System.out.println("BrowseName: " + browseName);
					System.out.println("Value: " + value.getValue().getValue());
					System.out.println("DataType: " + dataTypeId);
					System.out.println("=============================================");
				} else {
					System.out.println("Il nodeId inserito non si riferisce a un Nodo Variable!");
				}

				System.out.println("Vuoi Continuare a Leggere (y/n) ?");
				r = in.readLine();
			} while (r.equalsIgnoreCase("y"));
		}
		
		///////////// CREAZIONE DELLA SUBSCRIPTION /////////////
		System.out.println("\n//////////// Creazione della Suscription /////////////");
		System.out.println("\nPremi un tasto per continuare ");
		r = in.readLine();
		/* Un oggetto della classe ManagedSubscription fornisce API di alto livello per creare Subscription
		 * e MonitoredItems. 
		 * Per vedere come usare una API di basso livello per Subscription e MonitoredItem
		 * consultare: https://github.com/eclipse/milo/blob/master/milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/SubscriptionExample.java
		 * */
		Double Default_RequestedPublishingInterval = (double) 1000, requestedPublishingInterval;
		System.out.println("Requested Publishing Interval [DEFAULT: " + Default_RequestedPublishingInterval + "]:");
		try {
			requestedPublishingInterval = Double.parseDouble(in.readLine());
		} catch (Exception e) {
			requestedPublishingInterval = Default_RequestedPublishingInterval;
		}
		
		ManagedSubscription subscription = ManagedSubscription.create(client, requestedPublishingInterval);
		System.out.println("Subscription creata.");
		
		/* Una volta creato una ManagedSubscription devo associarvi un ChangeListener per poter gestire 
		 * le notifiche che inizierò a ricevere nel momento in cui registrerò dei MonitoredItem. */
		subscription.addChangeListener(new ChangeListener() {
			@Override
			public void onDataReceived(List<ManagedDataItem> dataItems, List<DataValue> dataValues) {
				/* Ogni oggetto nella lista dataItems ha un corrispondente valore allo
				 * stesso indice nella lista dataValues.
				 * Alcuni oggetti possono apparire più volte se l'oggetto ha una queue
				 * size maggiore di uno e il suo valore è cambiato più volte all'interno
				 * di un publishing interval della subscription.
				 * Gli oggetti e i valori appaiono in ordine di variazione. */
				for(int i=0; i<dataItems.size(); i++) {
					UInteger subId = subscription.getSubscription().getSubscriptionId();
					ManagedDataItem item = dataItems.get(i);
					DataValue value = dataValues.get(i);
					System.out.println("SubscriptionId: " + subId + " - NodeId: " + item.getNodeId() + " - Value: " + value.getValue().getValue() + " - SourceTimestamp: " + value.getSourceTime());
				}
				
			}
		});
		
		/* Adesso abbiamo creato una Subscription e definito cosa deve fare quando riceve Notification da eventuali
		 * MonitoredItems. Adesso creiamo un MonitoredItem. */
		Double Default_SamplingInterval = (double) 1000, samplingInterval;
		
		System.out.println("\nCreazione MonitoredItem per un Nodo Variable");
		System.out.println("Inserisci namespace  = ");
		ns = Integer.parseInt(in.readLine());
		System.out.println("Inserisci nodeId  = ");
		identifier = in.readLine();
		if(isNumeric(identifier)) {
			idNum = Integer.parseInt(identifier);
			nodeId = new NodeId(ns, idNum);
		} else {
			nodeId = new NodeId(ns, identifier);
		}
		
		ReadValueId redValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
		
		System.out.println("Inserire il sampling Interval [DEFAULT: 1000]:");
		try {
			samplingInterval = Double.parseDouble(in.readLine());
		} catch (Exception e) {
			samplingInterval = Default_SamplingInterval;
		}
		
		/* Una volta creato il MonitoredItem inizia a funzionare */
		ManagedDataItem dataItem = subscription.createDataItem(samplingInterval, redValueId);
		
		/* Controllare sempre lo StatusCode quando si crea o si modifica un MonitoredItem! */
		if (!dataItem.getStatusCode().isGood()) {
		    throw new RuntimeException("Errore nella creazione del MonitoredItem!");
		}
		
		Thread.sleep(5000);
		System.out.println("\nEsercizio completato. Cancello sottoscrizione e chiudo la sessione.");
		subscription.delete();
		client.disconnect();
		System.exit(0);
    }
}
