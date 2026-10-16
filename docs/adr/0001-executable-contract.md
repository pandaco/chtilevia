# Un contrat exécutable plutôt qu'une spécification

Chtipasse existe en trois implémentations : le script shell, le cœur Kotlin et le serveur MCP. Leur comportement n'est pas décrit par un document, mais par un contrat de cas figés (une commande, une heure, des flux enregistrés) que chaque implémentation doit reproduire au caractère près : sortie standard, erreurs et code retour (`libs/contract`). C'est le seul moyen d'empêcher que les versions divergent. En contrepartie, tout changement d'affichage impose de régénérer les références et de les relire.
