
todo:
- graphe execution to show graphicaly the advance
- result : status of robot (ok) AND status of execution : robot has to create 10 cases, how many case did he create ? Separate the execution and the result
- freeze all button : collect, load , save, refresh, start ==> when one operation is in progress, freeze everything
- run on an another server
- calcul cover : graph to display the result
- internal: rename constant in MeteorMain 
- graph througput : give the average of execution time to detect a performance issue


========================================================= Review
Probl�mes de la page actuelle:

- mix de personas: technique et moins technique

- spectre trop large: fonctionnel, perf, custom�

- environnement de la page � am�liorer (objectif, ce que c�est, ce que ce n�est pas�)


Proposition pour avancer:

- refaire une nouvelle page from scratch comme cela tu ne perturbes pas la communaut� avec la page existante

- r�duire le p�rim�tre au test fonctionnel

- rendre la page � clic clic only �, pas de code

- ajoute l�export � as a Jenkins job �


Solution envisag�e:

- cr�er l�API qui va bien pour r�cup�rer un enregistrement (contrats d�instance et de t�che) d�un case donn� correspondant � un enregistrement fait par un utilisateur sur la plateforme

- permettre dans la page de cr�er un sc�nario de test compos� de l�import de diff�rents cases depuis:

  * soit un case id directement

  * soit par s�lection d�un process par son nom & version puis s�lection du case gr�ce � certaines m�ta-donn�es (date de cr�ation, contrat d�instantiation�)

- permettre depuis la page de jouer le case ? Quel serait l�int�r�t?

- permettre depuis la page d�exporter le test fonctionnel en tant que job Jenkins qui exposerait juste le param�tre de � sleep � avant d�ex�cuter une t�che


Questions / doutes � creuser:

- persistenceId qui bougent pour un BDM? 


Proposition de texte pour cette nouvelle page:

- title: � d�finir si tu veux des noms � fun � mais dans ce cas, c�est cool d�expliquer le choix du nom�

- sub-title: � Jenkins job: process test generator �

- goal:  graphically create functional process tests and export them as Jenkins jobs

- how to use it: download and install this page in one of your non-production Bonita Runtime

