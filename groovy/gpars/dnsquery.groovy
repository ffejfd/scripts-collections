import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import org.xbill.DNS.*;

def queryName(String domainName, String server) {
	resolver = new SimpleResolver(server)
	resolver.setTimeout(5)
	name = Name.fromString(domainName, Name.root)
	record = Record.newRecord(name, Type.A, DClass.IN)
	query = Message.newQuery(record)
	response = resolver.send(query)
	return response
}

class Master extends DefaultActor {
	int success = 0
	int failure = 0

	void act() {
		loop {
			react {boolean status ->
				if (status)
					success ++
				else {
					println success
					terminate()
				}
			}
		}
	}
}

class Querier extends DefaultActor {
	String dnsServer
	Actor counter

	void act() {
		counter << true
		counter << false
	}
}
def master = new Master().start()
def querier = new Querier(dnsServer:'8.8.8.8', counter:master).start()
[querier, master]*.join()
//println queryName('www.baidu.com', '8.8.8.8')
