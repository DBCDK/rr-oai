
-- US#2707

UPDATE oaisets SET description='Poster fra base 870970 med en af følgende koder i felt 032x: BKM, BKR, BKX, AC*, SF*, NET, ITU, INV. Betalingsprodukt - kræver adgangskode.'
 WHERE setSpec='bkm';

UPDATE oaisets SET description='Poster fra base 870970 og 870971 der indeholder et delfelt a i felt 032. Offentligt tilgængeligt.'
 WHERE setSpec='nat';

UPDATE oaisets SET description='Poster fra base 870971 som IKKE har koden ''ANM'' i felt 014x. Offentligt tilgængeligt.'
 WHERE setSpec='art';

UPDATE oaisets SET description='Poster fra base 870970 som har koden ''xe'' i felt 009g ELLER et delfelt u i felt 856 og derudover har en af følgende koder felt 032a eller 032x: DBF, DPF, BKM, DAT, NEP, SNE, IDU. Desuden poster fra base 870970 med koden ''DAT'' i felt 032 og en af følgende koder i felt 032a eller 032x: IDO, IDP, NEP, NET, SNE. Betalingsprodukt - kræver adgangskode.'
 WHERE setSpec='onl';
