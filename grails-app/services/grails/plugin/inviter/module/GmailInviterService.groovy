package grails.plugin.inviter.module

import static groovyx.net.http.ContentType.URLENC
import grails.converters.XML

class GmailInviterService {

    static transactional = true

	static showCustomLogin = false
    static Url = "https://www.google.com"

    def login(email, password) {

        def authenticated = false;
        def authToken = ''

        withHttp(uri: Url) {

            post(
                    path: 'accounts/ClientLogin',
                    body: [
                            accountType: 'HOSTED_OR_GOOGLE',
                            Email: email,
                            Passwd: password,
                            service: 'cp',
                            source: 'Grails-Inviter-0.1'
                    ],
                    requestContentType: URLENC
            ) { resp, reader ->

                if (resp.statusLine.statusCode == 200) {
                    authenticated = true
                    reader.text.eachLine {
                        if (it.startsWith('Auth=')) {
                            authToken = it.split('=')[1]
                        }
                    }
                }

            }
        }
        return [authenticated, authToken]

    }

    def getContacts(authToken) {

        def parsedContacts = false

        withHttp(uri: Url) {
            get(
                    path: 'm8/feeds/contacts/default/full',
                    query: ['max-results': 10000],
                    headers: [Authorization: "GoogleLogin auth=${authToken}"]
            ) { resp, reader ->
                def contactsXml = XML.parse(reader.text)
                def contactList = []
				def contactListWithoutNames = []

				def addedContacts = []

                contactsXml.entry.each { entry ->

                    entry.email.each{ email ->

						def address = email.@address as String

						if( email.@primary == 'true' && !addedContacts.contains( address ) ){
							def contact = [ : ]
							if( !entry.title?.toString().isEmpty() ){
								contact.name = entry.title.toString()
							}
							contact.address = address

							if( contact.name ){
								contactList << contact
							} else {
								contactListWithoutNames << contact
							}

							addedContacts.add( address )
						}
                    }
                }

			 	parsedContacts = contactList.sort{ it.name.toLowerCase() } + contactListWithoutNames.sort{ it.address }

            }
            parsedContacts
        }

    }
}
