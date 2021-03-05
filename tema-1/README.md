[demo]: doc/demo.gif

[req-pdf]: req/req.pdf

[src]: src/
[util]: src/util/
[array-map-java]: src/util/ArrayMap.java
[file-scanner-java]: src/util/FileScanner.java
[util-java]: src/util/Util.java
[vms]: src/vms/
[campaign]: src/vms/campaign/
[campaign-java]: src/vms/campaign/Campaign.java
[campaign-status-type-java]: src/vms/campaign/CampaignStatusType.java
[campaign-strategy-type-java]: src/vms/campaign/CampaignStrategyType.java
[campaign-voucher-map-java]: src/vms/campaign/CampaignVoucherMap.java
[exception-java]: src/vms/exception/
[vms-access-exception-java]: src/vms/exception/VMSAccessException.java
[vms-argument-exception-java]: src/vms/exception/VMSArgumentException.java
[vms-not-found-exception-java]: src/vms/exception/VMSNotFoundException.java
[vms-parse-exception-java]: src/vms/exception/VMSParseException.java
[vms-state-exception-java]: src/vms/exception/VMSStateException.java
[notification]: src/vms/notification/
[notification-java]: src/vms/notification/Notification.java
[notification-type-java]: src/vms/notification/NotificationType.java
[ui]: src/vms/ui/
[admin-pane-java]: src/vms/ui/AdminPane.java
[guest-pane-java]: src/vms/ui/GuestPane.java
[launch-dialogs-java]: src/vms/ui/LaunchDialogs.java
[login-pane-java]: src/vms/ui/LoginPane.java
[no-border-table-cell-renderer-java]: src/vms/ui/NoBorderTableCellRenderer.java
[window-java]: src/vms/ui/Window.java
[user]: src/vms/user/
[user-java]: src/vms/user/User.java
[user-type-java]: src/vms/user/UserType.java
[user-voucher-map-java]: src/vms/user/UserVoucherMap.java
[voucher]: src/vms/voucher/
[gift-voucher-java]: src/vms/voucher/GiftVoucher.java
[loyalty-voucher-java]: src/vms/voucher/LoyaltyVoucher.java
[voucher-java]: src/vms/voucher/Voucher.java
[voucher-status-type-java]: src/vms/voucher/VoucherStatusType.java
[vms-java]: src/vms/

[lgooddatepicker]: https://github.com/LGoodDatePicker/LGoodDatePicker

# Voucher Management Service
Acest subdirector conține proiectul [Eclipse][eclipse] corespunzător **Temei 1** din cadrul cursului _Programare orientată pe obiecte_ ce presupune implementarea unei aplicații pentru gestionarea unor campanii promoționale cu vouchere. Programul este împărțit în mai multe componente/fișiere, fiecare din acestea rezolvând o anumită problemă. Mai jos se află o mică demonstrație a funcționalităților aplicației:

![demo]

## Cerințe
Cerințele acestei aplicații, pentru o înțelegere mai bună a demonstrației de mai sus, se găsesc în [req.pdf][req-pdf].

## Detalii de implementare
Am plecat de la testele furnizate orientativ și am implementat sistemul VMS fără interfață grafică, după care am legat interfața grafică la VMS.

## Codul sursă
Codul sursă al proiectului se găsește în [directorul src][src], componentele din care este format sistemul fiind următoarele:
* [`util`][util]: diverse clase auxiliare ce facilitează implementarea funcționalităților
  * [`ArrayMap`][array-map-java]
  * [`FileScanner`][file-scanner-java]
  * [`Util`][util-java]
* [`vms`][vms]: sistemul propriu-zis de gestionare a campaniilor și a voucherelor aferente pentru utilizatori
  * [`campaign`][campaign]: modelul unei campanii
    * [`Campaign`][campaign-java]
    * [`CampaignStatusType`][campaign-status-type-java]
    * [`CampaignStrategyType`][campaign-strategy-type-java]
    * [`CampaignVoucherMap`][campaign-voucher-map-java]
  * [`exception`][exception-java]: diverse excepții ce pot apărea în cadrul rulării aplicației
    * [`VMSAccessException`][vms-access-exception-java]
    * [`VMSArgumentException`][vms-argument-exception-java]
    * [`VMSNotFoundException`][vms-not-found-exception-java]
    * [`VMSParseException`][vms-parse-exception-java]
    * [`VMSStateException`][vms-state-exception-java]
  * [`notification`][notification]: notificări ce apar când se modifică campaniile
    * [`Notification`][notification-java]
    * [`NotificationType`][notification-type-java]
  * [`ui`][ui]: componenta de interfațare grafică cu utilizatorul; interfața grafică se află în totalitate în acest pachet; aceasta interacționează în mod direct cu clasa [`VMS`][vms-java] pentru
configurarea elementelor acesteia cu datele potrivite
    * [`AdminPane`][admin-pane-java]
    * [`GuestPane`][guest-pane-java]
    * [`LaunchDialogs`][launch-dialogs-java]
    * [`LoginPane`][login-pane-java]
    * [`NoBorderTableCellRenderer`][no-border-table-cell-renderer-java]
    * [`Window`][window-java]
    ### Notă
    Pentru accesarea meniului cu mai multe opțiuni după logare în interfața grafică se face _click_-dreapta pe un element din tabel.
  * [`user`][user]: modelul unui utilizator
    * [`User`][user-java]
    * [`UserType`][user-type-java]
    * [`UserVoucherMap`][user-voucher-map-java]
  * [`voucher`][voucher]: modelul unui voucher
    * [`GiftVoucher`][gift-voucher-java]
    * [`LoyaltyVoucher`][loyalty-voucher-java]
    * [`Voucher`][voucher-java]
    * [`VoucherStatusType`][voucher-status-type-java]
  * [`VMS`][vms-java]: are mai multe clase imbricate cu ajutorul cărora rezolvă diferite sarcini
    * `Reader`: clasă utilizată pentru citirea fișierelor text, fie ele fișiere de test (e.g. `campaigns.txt`, `users.txt`, `events.txt`) sau alte tipuri de fișiere (e.g. `emails.txt`)
    * `AccessController`: clasă utilizată pentru a verifica dacă utilizatorul care dorește să efectueze o acțiune are voie să o facă
    * `Test`: clasă utilizată pentru rularea testelor
    * `Config`: clasă utilizată pentru salvarea configurației interfeței grafice în fișierul `config.xml`

## Biblioteci utilizate
* [LGoodDatePicker 10.4.1][lgooddatepicker]: pentru realizarea meniului de alegere a datelor pentru
      începutul unei campanii, sfârșitul unei campanii etc. în cadrul interfeței grafice
