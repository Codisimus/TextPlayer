package com.codisimus.plugins.textplayer;

/**
 * A List of supported Carriers and their SMS Gateways
 *
 * @author Codisimus
 */
public class SMSGateways {
    public static enum Carrier { aircel, airtel, alaskacommunications, aliant,
    alltel, ameritech, aql, att, bell, bellsouth, beeline, bluegrass, bluesky,
    boostmobile, bouygues, cellcom, cellulaone, cellularsouth, centennialwireless,
    chinamobile, clarobrazil, cricket, csl, d1, edgewireless, eplus, esendexuk,
    esendexusa, esendexspain, etisalat, fido, freebiesms, googlevoice, koodo, lmt,
    metropcs, mobistar, mts, netcom, ntelos, optimus, optus, orange, o2germany,
    o2uk, o2usa, pcmobile, pioneer, rogers, sfr, softbank, sprint, starhub, sunrise,
    swisscom, tdc, telecom, telenor, tele2, telia, telstra, telus, three, tmn,
    tmobile, tmobileczech, tmobilegermany, tmobilenetherlands, uscellular,
    verizon, virginmobile, virginmobilecanada, vivo, vodafonegermany,
    vodafonegreece, vodafoneiceland, vodafoneitaly, vodafonespain,
    vodafonenewzealand, vodafoneuk, willcomjapan }
    
    public static String format(String number, Carrier carrier) {
        switch (carrier) {
        case aircel: return number+"@aircel.co.in";
        case airtel: return number+"@airtellkk.com";
        case alaskacommunications: return number+"@msg.acsalaska.com";
        case aliant: return number+"@sms.wirefree.informe.ca";
        case alltel: return number+"@sms.alltelwireless.com";
        case ameritech: return number+"@paging.acswireless.com";
        case aql: return number+"@text.aql.com";
        case att: return number+"@txt.att.net";
        case bell: return number+"@txt.bell.ca";
        case bellsouth: return number+"@bellsouth.cl";
        case beeline: return number+"@sms.beemail.ru";
        case bluegrass: return number+"@sms.bluecell.com";
        case bluesky: return number+"@psms.bluesky.as";
        case boostmobile: return number+"@myboostmobile.com";
        case bouygues: return number+"@mms.bouyguestelecom.fr";
        case cellcom: return number+"@cellcom.quiktxt.com";
        case cellulaone: return number+"@mobile.celloneusa.com";
        case cellularsouth: return number+"@csouth1.com";
        case centennialwireless: return number+"@cwemail.com";
        case chinamobile: return number+"@139.com";
        case clarobrazil: return number+"@clarotorpedo.com.br";
        case cricket: return number+"@sms.mycricket.com";
        case csl: return number+"@mgw.mmsc1.hkcsl.com";
        case d1: return number+"@t-d1-sms.de";
        case edgewireless: return number+"@sms.edgewireless.com";
        case eplus: return number+"@smsmail.eplus.de";
        case esendexuk: return number+"@echoemail.net";
        case esendexusa: return number+"@echoemail.net";
        case esendexspain: return number+"@esendex.net";
        case etisalat: return number+"@email2sms.ae";
        case fido: return number+"@fido.ca";
        case freebiesms: return number+"@smssturen.com";
        case googlevoice: return number+"@txt.voice.google.com";
        case koodo: return number+"@msg.telus.com";
        case lmt: return number+"@smsmail.lmt.lv";
        case metropcs: return number+"@mymetropcs.com";
        case mobistar: return number+"@mobistar.be";
        case mts: return number+"@text.mtsmobility.com";
        case netcom: return number+"@sms.netcom.no";
        case ntelos: return number+"@pcs.ntelos.com";
        case optimus: return number+"@sms.optimus.pt";
        case optus: return "0"+number+"@optusmobile.com.au";
        case orange: return number+"@orange.net";
        case o2germany: return "0"+number+"@o2online.de";
        case o2uk: return number+"@o2imail.co.uk";
        case o2usa: return number+"@mobile.celloneusa.com";
        case pcmobile: return number+"@mobiletxt.ca";
        case pioneer: return number+"@msg.pioneerenidcellular.com";
        case rogers: return number+"@pcs.rogers.com";
        case sfr: return number+"@sfr.fr";
        case softbank: return number+"@softbank.ne.jp";
        case sprint: return number+"@messaging.sprintpcs.com";
        case starhub: return number+"@starhub-enterprisemessaing.com";
        case sunrise: return number+"@mysunrise.ch";
        case swisscom: return number+"@bluewin.ch";
        case tdc: return number+"@sms.tdk.dk";
        case telecom: return number+"@etxt.co.nz";
        case telenor: return number+"@mobilpost.no";
        case tele2: return number+"@sms.tele2.lv";
        case telia: return number+"@gsm1800.telia.dk";
        case telstra: return number+"@tim.telstra.com";
        case telus: return number+"@msg.telus.com";
        case three: return number+"@three.co.uk";
        case tmn: return number+"@mail.tmn.pt";
        case tmobile: return number+"@tmomail.net";
        case tmobileczech: return number+"@sms.paegas.cz";
        case tmobilegermany: return number+"@t-mobile-sms.de";
        case tmobilenetherlands: return "31"+number+"@gin.nl";
        case uscellular: return number+"@email.uscc.net";
        case verizon: return number+"@vtext.com";
        case virginmobile: return number+"@vmobl.com";
        case virginmobilecanada: return number+"@vmobile.ca";
        case vivo: return number+"@torpedoemail.com.br";
        case vodafonegermany: return "0"+number+"@vodafone-sms.de";
        case vodafonegreece: return number+"@sms.vodafone.gr";
        case vodafoneiceland: return number+"@mms.mymeteor.ie";
        case vodafoneitaly: return number+"@sms.vodafone.it";
        case vodafonespain: return "0"+number+"@vodafone.es";
        case vodafonenewzealand: return number+"@mtxt.co.nz";
        case vodafoneuk: return number+"@vodafone.net";
        case willcomjapan: return number+"@pdx.ne.jp";
        default: return "Unexpected Error: "+carrier.name();
        }
    }
}
