/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.utils.StringDatabaseKDBUtils
import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Output the GroupKDB to the stream
 */
class EntryOutputKDB {

    companion object {
        @Throws(DatabaseOutputException::class)
        fun writeEntry(mOutputStream: OutputStream,
                       mEntry: EntryKDB,
                       binaryCipherKey: Database.LoadedKey) {
            //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int
            try {
                // UUID
                mOutputStream.write(UUID_FIELD_TYPE)
                mOutputStream.write(UUID_FIELD_SIZE)
                mOutputStream.write(uuidTo16Bytes(mEntry.id))

                // Group ID
                mOutputStream.write(GROUPID_FIELD_TYPE)
                mOutputStream.write(GROUPID_FIELD_SIZE)
                mOutputStream.write(uIntTo4Bytes(UnsignedInt(mEntry.parent!!.id)))

                // Image ID
                mOutputStream.write(IMAGEID_FIELD_TYPE)
                mOutputStream.write(IMAGEID_FIELD_SIZE)
                mOutputStream.write(uIntTo4Bytes(UnsignedInt(mEntry.icon.iconId)))

                // Title
                //byte[] title = mEntry.title.getBytes("UTF-8");
                mOutputStream.write(TITLE_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToBytes(mEntry.title, mOutputStream).toLong()

                // URL
                mOutputStream.write(URL_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToBytes(mEntry.url, mOutputStream).toLong()

                // Username
                mOutputStream.write(USERNAME_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToBytes(mEntry.username, mOutputStream).toLong()

                // Password
                mOutputStream.write(PASSWORD_FIELD_TYPE)
                writePassword(mOutputStream, mEntry.password).toLong()

                // Additional
                mOutputStream.write(ADDITIONAL_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToBytes(mEntry.notes, mOutputStream).toLong()

                // Create date
                writeDate(mOutputStream, CREATE_FIELD_TYPE, dateTo5Bytes(mEntry.creationTime.date))

                // Modification date
                writeDate(mOutputStream, MOD_FIELD_TYPE, dateTo5Bytes(mEntry.lastModificationTime.date))

                // Access date
                writeDate(mOutputStream, ACCESS_FIELD_TYPE, dateTo5Bytes(mEntry.lastAccessTime.date))

                // Expiration date
                writeDate(mOutputStream, EXPIRE_FIELD_TYPE, dateTo5Bytes(mEntry.expiryTime.date))

                // Binary description
                mOutputStream.write(BINARY_DESC_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToBytes(mEntry.binaryDescription, mOutputStream).toLong()

                // Binary
                mOutputStream.write(BINARY_DATA_FIELD_TYPE)
                val binaryData = mEntry.binaryData
                val binaryDataLength = binaryData?.length() ?: 0L
                // Write data length
                mOutputStream.write(uIntTo4Bytes(UnsignedInt.fromKotlinLong(binaryDataLength)))
                // Write data
                if (binaryDataLength > 0) {
                    binaryData?.getInputDataStream(binaryCipherKey).use { inputStream ->
                        inputStream?.readBytes(DatabaseKDB.BUFFER_SIZE_BYTES) { buffer ->
                            mOutputStream.write(buffer)
                        }
                        inputStream?.close()
                    }
                }

                // End
                mOutputStream.write(END_FIELD_TYPE)
                mOutputStream.write(ZERO_FIELD_SIZE)
            } catch (e: IOException) {
                throw DatabaseOutputException("Failed to output an entry.", e)
            }
        }

        @Throws(IOException::class)
        private fun writeDate(outputStream: OutputStream,
                              type: ByteArray,
                              date: ByteArray?) {
            outputStream.write(type)
            outputStream.write(DATE_FIELD_SIZE)
            if (date != null) {
                outputStream.write(date)
            } else {
                outputStream.write(ZERO_FIVE)
            }
        }

        @Throws(IOException::class)
        private fun writePassword(outputStream: OutputStream, str: String): Int {
            val initial = str.toByteArray(Charset.forName("UTF-8"))
            val length = initial.size + 1
            outputStream.write(uIntTo4Bytes(UnsignedInt(length)))
            outputStream.write(initial)
            outputStream.write(0x00)
            return length
        }

        // Constants
        private val UUID_FIELD_TYPE:ByteArray = uShortTo2Bytes(1)
        private val GROUPID_FIELD_TYPE: ByteArray = uShortTo2Bytes(1)
        private val IMAGEID_FIELD_TYPE:ByteArray = uShortTo2Bytes(3)
        private val TITLE_FIELD_TYPE:ByteArray = uShortTo2Bytes(4)
        private val URL_FIELD_TYPE:ByteArray = uShortTo2Bytes(5)
        private val USERNAME_FIELD_TYPE:ByteArray = uShortTo2Bytes(6)
        private val PASSWORD_FIELD_TYPE:ByteArray = uShortTo2Bytes(7)
        private val ADDITIONAL_FIELD_TYPE:ByteArray = uShortTo2Bytes(8)
        private val CREATE_FIELD_TYPE:ByteArray = uShortTo2Bytes(9)
        private val MOD_FIELD_TYPE:ByteArray = uShortTo2Bytes(10)
        private val ACCESS_FIELD_TYPE:ByteArray = uShortTo2Bytes(11)
        private val EXPIRE_FIELD_TYPE:ByteArray = uShortTo2Bytes(12)
        private val BINARY_DESC_FIELD_TYPE:ByteArray = uShortTo2Bytes(13)
        private val BINARY_DATA_FIELD_TYPE:ByteArray = uShortTo2Bytes(14)
        private val END_FIELD_TYPE:ByteArray = uShortTo2Bytes(0xFFFF)

        private val LONG_FOUR:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val UUID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(16))
        private val GROUPID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val DATE_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(5))
        private val IMAGEID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val LEVEL_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val FLAGS_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val ZERO_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(0))
        private val ZERO_FIVE:ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
    }
}
