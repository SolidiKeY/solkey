// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: nested fixed/dynamic array shapes, copied across storage and memory.
// Conventions: README.md, "Writing an example".
contract NestedArrays {
    uint[2][2] ff;
    uint[2][2] ffTarget;
    uint[2][] df;
    uint[][2] fd;
    uint[][1] fdShort;
    uint[][] dd;
    uint[][] ddTarget;
    uint[2][2] smallGrid;
    uint[3][3] bigGrid;
    uint[2][][2] fdf;
    uint[2][][2] fdfTarget;
    uint[][2][] dfd;
    uint[][2][] dfdTarget;

    function fixedMatrixStorageToMemoryIsDeep() public {
        delete ff;
        ff[0][0] = 11;
        ff[1][1] = 22;
        uint[2][2] memory m = ff;
        ff[0][0] = 99;
        assert(m[0][0] == 11);
        m[1][1] = 77;
        assert(ff[1][1] == 22);
    }

    function fixedMatrixMemoryToStorageIsDeep() public {
        uint[2][2] memory m;
        m[0][0] = 11;
        m[1][1] = 22;
        delete ffTarget;
        ffTarget[0][0] = 71;
        ffTarget[1][1] = 82;
        ffTarget = m;
        m[0][0] = 99;
        assert(ffTarget[0][0] == 11);
        ffTarget[1][1] = 77;
        assert(m[1][1] == 22);
    }

    function fixedMatrixSharedMemoryRowsCopyApart() public {
        uint[2][2] memory m;
        m[0][0] = 11;
        m[1][0] = 21;
        m[1] = m[0];
        m[0][0] = 41;
        assert(m[1][0] == 41);
        delete ffTarget;
        ffTarget[1][0] = 81;
        ffTarget = m;
        ffTarget[0][0] = 99;
        assert(ffTarget[1][0] == 41);
        assert(m[0][0] == 41);
    }

    function dynamicMatrixMemoryToStorageIsDeep() public {
        uint[][] memory m = new uint[][](2);
        m[0] = new uint[](2);
        m[0][0] = 11;
        m[1] = new uint[](2);
        m[1][1] = 22;
        delete ddTarget;
        ddTarget.push();
        ddTarget[0].push(71);
        ddTarget = m;
        m[0][0] = 99;
        assert(ddTarget.length == 2);
        assert(ddTarget[0].length == 2);
        assert(ddTarget[0][0] == 11);
        ddTarget[1][1] = 77;
        assert(m[1][1] == 22);
    }

    function dynamicMatrixSharedMemoryRowsCopyApart() public {
        uint[][] memory m = new uint[][](2);
        m[0] = new uint[](2);
        m[0][0] = 11;
        m[1] = new uint[](2);
        m[1][0] = 21;
        m[1] = m[0];
        m[0][0] = 41;
        assert(m[1][0] == 41);
        delete ddTarget;
        ddTarget = m;
        ddTarget[0][0] = 99;
        assert(ddTarget[1][0] == 41);
        assert(m[0][0] == 41);
    }

    function fixedMatrixIntoDynamicMatrix() public {
        delete ff;
        ff[0][0] = 11;
        ff[0][1] = 12;
        delete dd;
        dd.push();
        dd[0].push(71);
        dd = ff;
        assert(dd.length == 2);
        assert(dd[0].length == 2);
        assert(dd[0][0] == 11);
        dd[0].pop();
        dd[0].push();
        assert(dd[0][1] == 0);
        assert(ff[0][1] == 12);
    }

    function fixedOuterIntoDynamicOuter() public {
        delete fd;
        fd[0].push(11);
        fd[1].push(22);
        delete dd;
        dd.push();
        dd[0].push(71);
        dd = fd;
        fd[0][0] = 99;
        assert(dd.length == 2);
        assert(dd[0].length == 1);
        assert(dd[0][0] == 11);
        assert(dd[1].length == 1);
        assert(dd[1][0] == 22);
    }

    function fixedMemoryMatrixIntoDynamicRows() public {
        uint[2][2] memory m;
        m[0][0] = 11;
        m[1][1] = 22;
        delete df;
        df.push();
        df[0][0] = 71;
        df = m;
        m[0][0] = 99;
        assert(df.length == 2);
        assert(df[0].length == 2);
        assert(df[0][0] == 11);
        assert(df[1][1] == 22);
    }

    function shorterFixedOuterEmptiesDynamicRow() public {
        delete fdShort;
        fdShort[0].push(11);
        fdShort[0].push(12);
        delete fd;
        fd[1].push(81);
        fd[1].push(82);
        fd = fdShort;
        assert(fd.length == 2);
        assert(fd[0].length == 2);
        assert(fd[0][0] == 11);
        assert(fd[1].length == 0);
        fd[1].push();
        assert(fd[1][0] == 0);
    }

    function nestedFixedCopyClearsInnerAndOuterTails() public {
        smallGrid[0][1] = 12;
        smallGrid[1][1] = 22;
        bigGrid[0][2] = 93;
        bigGrid[1][2] = 94;
        bigGrid[2][0] = 95;
        bigGrid[2][2] = 96;
        bigGrid = smallGrid;
        assert(bigGrid.length == 3);
        assert(bigGrid[2].length == 3);
        assert(bigGrid[0][1] == 12);
        assert(bigGrid[1][1] == 22);
        assert(bigGrid[0][2] == 0);
        assert(bigGrid[1][2] == 0);
        assert(bigGrid[2][0] == 0);
        assert(bigGrid[2][2] == 0);
    }

    function cubeStorageCopyIsDeep() public {
        delete fdf;
        fdf[0].push();
        fdf[0][0][0] = 11;
        fdf[1].push();
        fdf[1].push();
        fdf[1][1][1] = 122;
        delete fdfTarget;
        fdfTarget[0].push();
        fdfTarget[0].push();
        fdfTarget[0][0][0] = 71;
        fdfTarget = fdf;
        fdf[0][0][0] = 99;
        assert(fdfTarget[0].length == 1);
        assert(fdfTarget[0][0][0] == 11);
        fdfTarget[1][1][1] = 77;
        assert(fdf[1][1][1] == 122);
    }

    function cubeRowCopyIsDeep() public {
        delete fdf;
        fdf[0].push();
        fdf[0].push();
        fdf[0][0][0] = 11;
        fdf[1].push();
        fdf[1][0][1] = 122;
        fdf[1] = fdf[0];
        fdf[0][0][0] = 99;
        assert(fdf[1].length == 2);
        assert(fdf[1][0][0] == 11);
    }

    function cubeStorageToMemoryIsDeep() public {
        delete fdf;
        fdf[0].push();
        fdf[0][0][0] = 11;
        fdf[1].push();
        fdf[1].push();
        fdf[1][1][1] = 122;
        uint[2][][2] memory m = fdf;
        fdf[0][0][0] = 99;
        assert(m[0][0][0] == 11);
        m[1][1][1] = 77;
        assert(fdf[1][1][1] == 122);
    }

    function cubeMemoryToStorageIsDeep() public {
        uint[2][][2] memory m;
        m[0] = new uint[2][](1);
        m[0][0][0] = 11;
        m[1] = new uint[2][](2);
        m[1][1][1] = 122;
        delete fdfTarget;
        fdfTarget[1].push();
        fdfTarget[1][0][1] = 182;
        fdfTarget = m;
        m[0][0][0] = 99;
        assert(fdfTarget[1].length == 2);
        assert(fdfTarget[0][0][0] == 11);
        fdfTarget[1][1][1] = 77;
        assert(m[1][1][1] == 122);
    }

    function dynamicCubeStorageToMemoryIsDeep() public {
        delete dfd;
        dfd.push();
        dfd.push();
        dfd[0][0].push();
        dfd[0][0][0] = 11;
        dfd[1][1].push();
        dfd[1][1].push();
        dfd[1][1][1] = 122;
        uint[][2][] memory m = dfd;
        dfd[0][0][0] = 99;
        assert(m[0][0][0] == 11);
        m[1][1][1] = 77;
        assert(dfd[1][1][1] == 122);
    }

    function dynamicCubeMemoryToStorageIsDeep() public {
        uint[][2][] memory m = new uint[][2][](2);
        m[0][0] = new uint[](1);
        m[0][0][0] = 11;
        m[1][1] = new uint[](2);
        m[1][1][1] = 122;
        delete dfdTarget;
        dfdTarget.push();
        dfdTarget[0][0].push(71);
        dfdTarget = m;
        m[0][0][0] = 99;
        assert(dfdTarget.length == 2);
        assert(dfdTarget[0][0][0] == 11);
        dfdTarget[1][1][1] = 77;
        assert(m[1][1][1] == 122);
    }

    function dynamicCubeSharedMemoryRowsCopyApart() public {
        uint[][2][] memory m = new uint[][2][](2);
        m[0][0] = new uint[](1);
        m[0][0][0] = 11;
        m[1] = m[0];
        m[0][0][0] = 41;
        assert(m[1][0][0] == 41);
        delete dfdTarget;
        dfdTarget = m;
        dfdTarget[0][0][0] = 99;
        assert(dfdTarget[1][0][0] == 41);
        assert(m[0][0][0] == 41);
    }

    function dynamicCubeDeleteResetsEveryLevel() public {
        delete dfd;
        dfd.push();
        dfd.push();
        dfd[0][0].push(11);
        dfd[1][1].push(122);
        delete dfd;
        assert(dfd.length == 0);
        dfd.push();
        assert(dfd[0].length == 2);
        assert(dfd[0][0].length == 0);
        dfd[0][0].push();
        assert(dfd[0][0][0] == 0);
    }

    function dynamicCubePushCopiesNestedElement() public {
        delete dfd;
        dfd.push();
        delete dfdTarget;
        dfdTarget.push();
        dfdTarget[0][0].push();
        dfdTarget[0][0][0] = 11;
        dfdTarget[0][1].push();
        dfdTarget[0][1][0] = 12;
        dfd.push(dfdTarget[0]);
        dfdTarget[0][0][0] = 99;
        assert(dfd.length == 2);
        assert(dfd[1][0][0] == 11);
        assert(dfd[1][1][0] == 12);
    }
}
