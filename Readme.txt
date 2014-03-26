Tema 2 APD

Stoican Elena-Andreea 332 CB.

Tema a fost realizata in Eclipse pornind de la arhiva din cadrul laboratorului 5.

Pentru rezolvarea temei s-a folosit modelul Replicated Workers.

Initial, pentru fiecare document s-a realizat impartirea lui in bucati de cate D fragmente. A fost creat un WorkPool in cara au fost adaugate task-uri. Aceste task-uri reprezinta primirea unui fragment din fisier si determinarea numarului de aparitie a fiecarui cuvant din fragmentul respectiv, care au fost retinute intr-un HashMap de forma (cuvant, numar de aparitii). Pentru fiecare document aceste perechi au fost retinute intr-o lista, iar aceste liste au fost retinute intr-un HashMap de forma (document, lista de HashMap-uri(explicata mai sus)).

Rezultatul obtinut anterior este folosit pentru determinarea frecventei de aparitie a fiecarui cuvant din fiecare fisier in operatia "Reduce".
Astfel, se creeaza un nou WorkPool cu task-uri de tip reduce. Fiecare task reprezinta calcularea frecventelor pentru cuvintele dintr-un fisier, deci fiecare task primeste o lista cu HashMap-uri de forma (cuvant, numar aparitii) - fiecare HashMap caracterizeaza un fragment din fisierul de procesat. Pentru procesarea task-ului se calculeaza initial numarul total de aparitii ale cuvintelor in intregul fisier (se aduna numarul de aparitii din fiecare fragment), si dupa asta se calculeaza frecventa fiecarui cuvant folosind formula din enuntul temei.
Rezultatul intors este de forma (nume_document, lista cu frecventele de aparitie a fiecarui cuvant).

Rezultatele obtinute anterior sunt utilizate pentru aflarea gradului de similaritate pentru fiecare document.
Astfel este creat din nou un WorkPool in care un task reprezinta procesul de comparare a doua documente, plecand de la HashMap-urile cu elemente (cuvant, frecventa aparitie), pentru a afla similaritatea dintre ele. Deci fiecare task o sa primeasca HashMap-ul documentului principal (pentru care se doreste sa se verifice daca este plagiat) si HashMap-ul unui document din lista de documente indexate.

Dupa ce se afla gradul de similaritate intre documentul principal si fiecare document indexat se sorteaza rezultatele si se scriu in fisierul de iesire doar acelea cu gradul de similaritate >= pragul minim (specificat in fisierul de intrare).
