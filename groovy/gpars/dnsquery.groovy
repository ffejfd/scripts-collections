import groovy.time.*
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.group.DefaultPGroup
@Grab(group='dnsjava', module='dnsjava', version='2.1.1')
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
}

class Querier extends DefaultActor {
	String dnsServer
	Actor counter
	
	void act() {
		counter << queryName('www.baidu.com', dnsServer)
	}

	def queryName(String domainName, String server) {
		def retries = 3		
		def resolver = new SimpleResolver(server)
		resolver.setTimeout(2)
		def name = Name.fromString(domainName, Name.root)
		def record = Record.newRecord(name, Type.A, DClass.IN)
		def query = Message.newQuery(record)
		def rcode = 100
		while (retries > 0) {
			try {
				rcode = resolver.send(query).getRcode()
				if (rcode == Rcode.NOERROR){
					//println server
					return rcode
				}
				retries--
			}catch(SocketTimeoutException e){
				retries--
				rcode = 100
			}catch(PortUnreachableException e) {
				retries --
				rcode = 100
			}catch(IOException e) {
				if (e.getMessage().equals('Too many open files'))
					throw new RuntimeException('Too many open files')
				retries --
				rcode = 100
			}
		}
		return rcode
	}
}

def timeStart = new Date()

def master = new Master().start()
final MAX_CONCURRENCY = args[0].toInteger()
Actors.defaultActorPGroup.resize  MAX_CONCURRENCY*2
def queue = []
new File('test.txt').withReader {reader ->
	def count = 0
	def cnt = MAX_CONCURRENCY
	while ((server = reader.readLine()) != null) {
		def querier = new Querier(dnsServer:server, counter:master).start()
		queue.push(querier)
		if (++count > cnt) {
			queue*.join()
			queue.clear()
			count = 0
		}
	}
}
if (!queue.isEmpty())
	queue*.join()
master.stop()
master.join()

def timeStop = new Date()
TimeDuration duration = TimeCategory.minus(timeStop, timeStart)
println "Duration: $duration"
