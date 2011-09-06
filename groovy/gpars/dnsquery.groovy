import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.group.DefaultPGroup
//@Grab(group='dnsjava', module='dnsjava', version='2.1.1')
import org.xbill.DNS.*;



class Master extends DefaultActor {
	int success = 0
	int nodomain = 0
	int serverNotConnected = 0
	int failure = 0

	void act() {
		loop {
			react {int status ->
				switch (status) {
					case Rcode.NOERROR: 
						success ++
						break
					case Rcode.NXDOMAIN:
						nodomain ++
						break
					case 100:
						serverNotConnected ++
						break
					default:
						failure ++
				}
			}
		}
	}

	void afterStop(List undeliveredMessages) {
		println "success: $success "
		println "domain not exists : $nodomain"
		println "server not connected : $serverNotConnected"
		println "failure: $failure "
	}

	void afterStart() {
	}
}

class Querier extends DefaultActor {
	String dnsServer
	Actor counter
	
	void act() {
		try {
			def response = queryName('www.baidu.com', dnsServer)
			def rcode = response.getRcode()
			counter << rcode
		}catch(SocketTimeoutException e){
			counter << 100
		}catch(PortUnreachableException e) {
			counter << 100
		}
	}

	def queryName(String domainName, String server) {
		def resolver = new SimpleResolver(server)
		resolver.setTimeout(5)
		def name = Name.fromString(domainName, Name.root)
		def record = Record.newRecord(name, Type.A, DClass.IN)
		def query = Message.newQuery(record)
		def response = resolver.send(query)
		return response
	}
}

def master = new Master().start()

Actors.defaultActorPGroup.resize 2000
def queue = []
new File('test.txt').eachLine {server ->
	def querier = new Querier(dnsServer:server, counter:master).start()
	queue.push(querier)
}

queue*.join()
master.stop()
master.join()
