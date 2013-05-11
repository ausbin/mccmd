# Maintainer: Austin Adams <screamingmoron@gmail.com>
pkgname=mccmd
pkgver=0.1
pkgrel=1
pkgdesc="mccmd client and systemd service"
arch=("any")
url="https://github.com/ausbin/mccmd/"
license=('MIT')
depends=("python")
optdepends=("systemd: for using the bundled service file")
source=("http://206.253.166.8/~austin/builds/mccmd-${pkgver}.tar.xz")
md5sums=("d7f05fdaec45ad4efff671a8dcbc4a58")

package() {
    install -Dm 644 $srcdir/minecraft.service $pkgdir/usr/lib/systemd/system/minecraft.service
    install -Dm 755 $srcdir/mccmd.py $pkgdir/usr/bin/mccmd
}
