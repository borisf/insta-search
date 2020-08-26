 /*
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.borisfarber.instasearch.controllers;

 import com.borisfarber.instasearch.ui.InstaSearch;

 import javax.swing.*;
 import java.awt.datatransfer.DataFlavor;
 import java.io.File;
 import java.util.List;

 public class FileTransfer extends TransferHandler {
     private final InstaSearch instaSearch;

     public FileTransfer(InstaSearch instaSearch) {
         this.instaSearch = instaSearch;
     }

     @Override
     public boolean canImport(TransferHandler.TransferSupport ts) {
         return true;
     }

     @Override
     public boolean importData(TransferHandler.TransferSupport ts) {
         try {
             List files = (List) ts.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
             instaSearch.onFileDragged((File)files.get(0));
             return true;
         } catch (Exception e) {
             e.printStackTrace();
         }
         return false;
     }
 }
