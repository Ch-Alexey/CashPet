# CashPet cat assets

The supplied `cats_source.svg` is the source sheet provided for this project.
From it the UI uses three visually distinct silhouettes/poses (`fluffy`, `smooth`, `lop`)
and three source colours (`grey`, `ginger`, `black`). Each is exported in three visual
stages (`s1`, `s2`, `s3`) by consistent scaling so the character grows without changing
its identity.

Generated files live in `app/src/main/res/drawable/` as:
`cat_<breed>_<colour>_<stage>.png`.

The source sheet does not contain explicit age variants, so the three stages are a
presentation treatment of the same source character rather than newly invented anatomy.
