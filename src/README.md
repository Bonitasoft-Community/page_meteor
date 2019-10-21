
todo:
- graphe execution to show graphicaly the advance
- result : status of robot (ok) AND status of execution : robot has to create 10 cases, how many case did he create ? Separate the execution and the result
- freeze all button : collect, load , save, refresh, start ==> when one operation is in progress, freeze everything
- run on an another server
- calcul cover : graph to display the result
- internal: rename constant in MeteorMain 
- graph througput : give the average of execution time to detect a performance issue


========================================================= Review
Problèmes de la page actuelle:

- mix de personas: technique et moins technique

- spectre trop large: fonctionnel, perf, custom…

- environnement de la page à améliorer (objectif, ce que c’est, ce que ce n’est pas…)


Proposition pour avancer:

- refaire une nouvelle page from scratch comme cela tu ne perturbes pas la communauté avec la page existante

- réduire le périmètre au test fonctionnel

- rendre la page « clic clic only », pas de code

- ajoute l’export « as a Jenkins job »


Solution envisagée:

- créer l’API qui va bien pour récupérer un enregistrement (contrats d’instance et de tâche) d’un case donné correspondant à un enregistrement fait par un utilisateur sur la plateforme

- permettre dans la page de créer un scénario de test composé de l’import de différents cases depuis:

  * soit un case id directement

  * soit par sélection d’un process par son nom & version puis sélection du case grâce à certaines méta-données (date de création, contrat d’instantiation…)

- permettre depuis la page de jouer le case ? Quel serait l’intérêt?

- permettre depuis la page d’exporter le test fonctionnel en tant que job Jenkins qui exposerait juste le paramètre de « sleep » avant d’exécuter une tâche


Questions / doutes à creuser:

- persistenceId qui bougent pour un BDM? 


Proposition de texte pour cette nouvelle page:

- title: à définir si tu veux des noms « fun » mais dans ce cas, c’est cool d’expliquer le choix du nom…

- sub-title: « Jenkins job: process test generator »

- goal:  graphically create functional process tests and export them as Jenkins jobs

- how to use it: download and install this page in one of your non-production Bonita Runtime

