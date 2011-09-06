
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor

//@Grab(group='dnsjava', module='dnsjava', version='2.1.1')
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
				else
					failure ++
			}
		}
	}

	void onStop() {
		println success
	}

	void a
}

class Querier extends DefaultActor {
	String dnsServer
	Actor counter

	void act() {
		counter << true
	}
}
def master = new Master().start()
def querier = new Querier(dnsServer:'202.120.224.6', counter:master).start()
[querier]*.join()
master.stop()
//println queryName('www.baidu.com', '8.8.8.8')
