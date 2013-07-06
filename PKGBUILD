# Maintainer: Austin Adams <screamingmoron@gmail.com>
pkgname=mccmd
pkgver=0.0.0
pkgrel=1
pkgdesc="mccmd client"
arch=("any")
url="https://github.com/ausbin/mccmd/"
license=("MIT")
depends=("readline")
source=("$pkgname::git+https://github.com/ausbin/mccmd.git")
md5sums=('SKIP')

pkgver () {
    cd $pkgname
    git describe --always
}

build () {
    cd $pkgname
    make mccmd
}

package() {
    cd $pkgname
    install -Dm 755 $srcdir/$pkgname/mccmd $pkgdir/usr/bin/mccmd
}
