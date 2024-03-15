#!/usr/bin/python3
"""Convert the standalone MF2 files to ICU ones."""

import os
import pathlib
import shutil

def process_file(file_name):
  count = 0
  tmp_file = str(file_name) + '.crap_tmp'
  with open(file_name, mode='r', encoding='utf-8') as in_file:
    with open(tmp_file, mode='w', encoding='utf-8') as out_file:
      for line in in_file:
        if 'message2x' in line:
          count += 1
          out_file.write(line.replace('message2x', 'message2'))
        else:
          out_file.write(line)
  if count != 0:
    shutil.move(tmp_file, file_name)
    print (f'\x1b[32m{file_name} : {count}\x1b[m')
  else:
    os.remove(tmp_file)
    print (f'\x1b[90m{file_name} : {count}\x1b[m')
  return count

def main():
  trgdir = 'src_icu'
  print (f'Remove previous folder ${trgdir}')
  shutil.rmtree(trgdir, ignore_errors=True)
  print (f'Copy src/ to ${trgdir}')
  shutil.copytree('src/', trgdir)
  print (f'Process ${trgdir}')
  for elem in pathlib.Path(trgdir).rglob('*.java'):
    process_file(elem)
  print ('\nReady to compare!\n')
  print ('  kdiff \\\n'
      '    src_icu/main/java/com/ibm/icu/message2x/ \\\n'
      '    $ICU_ROOT/icu4j/main/core/src/main/java/com/ibm/icu/message2/')
  print ('  kdiff \\\n'
      '    src_icu/test/java/com/ibm/icu/dev/test/message2/ \\\n'
      '    $ICU_ROOT/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/message2/')
  print ('  kdiff \\\n'
      '    src_icu/test/resources/com/ibm/icu/dev/test/message2/ \\\n'
      '    $ICU_ROOT/icu4j/main/core/src/test/resources/com/ibm/icu/dev/test/message2/')
  print ('  kdiff \\\n'
      '    src_icu/test/java/com/ibm/icu/dev/test/message2/ \\\n'
      '    $ICU_ROOT/icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/message2/')

main()
