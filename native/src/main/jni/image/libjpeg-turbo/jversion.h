/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * EhViewer. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * jversion.h
 *
 * This file was part of the Independent JPEG Group's software:
 * Copyright (C) 1991-2020, Thomas G. Lane, Guido Vollbeding.
 * libjpeg-turbo Modifications:
 * Copyright (C) 2010, 2012-2022, D. R. Commander.
 * For conditions of distribution and use, see the accompanying README.ijg
 * file.
 *
 * This file contains software version identification.
 */

#if JPEG_LIB_VERSION >= 80

#define JVERSION        "8d  15-Jan-2012"

#elif JPEG_LIB_VERSION >= 70

#define JVERSION        "7  27-Jun-2009"

#else

#define JVERSION        "6b  27-Mar-1998"

#endif

/*
 * NOTE: It is our convention to place the authors in the following order:
 * - libjpeg-turbo authors (2009-) in descending order of the date of their
 *   most recent contribution to the project, then in ascending order of the
 *   date of their first contribution to the project, then in alphabetical
 *   order
 * - Upstream authors in descending order of the date of the first inclusion of
 *   their code
 */

#define JCOPYRIGHT \
  "Copyright (C) 2009-2022 D. R. Commander\n" \
  "Copyright (C) 2015, 2020 Google, Inc.\n" \
  "Copyright (C) 2019-2020 Arm Limited\n" \
  "Copyright (C) 2015-2016, 2018 Matthieu Darbois\n" \
  "Copyright (C) 2011-2016 Siarhei Siamashka\n" \
  "Copyright (C) 2015 Intel Corporation\n" \
  "Copyright (C) 2013-2014 Linaro Limited\n" \
  "Copyright (C) 2013-2014 MIPS Technologies, Inc.\n" \
  "Copyright (C) 2009, 2012 Pierre Ossman for Cendio AB\n" \
  "Copyright (C) 2009-2011 Nokia Corporation and/or its subsidiary(-ies)\n" \
  "Copyright (C) 1999-2006 MIYASAKA Masaru\n" \
  "Copyright (C) 1991-2020 Thomas G. Lane, Guido Vollbeding"

#define JCOPYRIGHT_SHORT \
  "Copyright (C) 1991-2022 The libjpeg-turbo Project and many others"
