import sys
import argparse
import xml.etree.ElementTree as et
from cqs_dictionary import ploPropertiesDict, cdmPropertiesDict
    

#reading XML file
def read_custom_query():
    
    custom_queries = {}
    mytree = et.parse('/home/comprehend/repo/query/custom_queries.xml')
    myroot = mytree.getroot()


    for elem in myroot:
        query_id=elem.find('id').text
        query_value=elem.find('value').text
        query_value=query_value.replace('\n',' ')
        query_value=query_value.replace('\t',' ')
        custom_queries[query_id]=query_value
    
    return (custom_queries)

#Reading list of USDM Tables as defined in cqs dictionary
def read_usdm_list():
    usdm_tables_list =[]
    for obj in (cdmPropertiesDict):
        usdm_tables_list.append(obj)
    
    return (usdm_tables_list)

#Reading list of PLO Tables as defined in cqs dictionary
def read_plo_list():
    plo_tables_list =[]
    for obj in (ploPropertiesDict):
        plo_tables_list.append(obj)
    
    return (plo_tables_list)
    
def main(argv):
     
     if sys.argv[1] == 'u':
         usdm_tables = read_usdm_list()
         print (','.join(usdm_tables))
     
     elif sys.argv[1] == 'p':
         plo_tables = read_plo_list()
         print (','.join(plo_tables))
     
     elif sys.argv[1] == 'c':
         custom_queries = read_custom_query()
         for key in custom_queries:
             print (key +'#' + custom_queries[key])
        
     else:
         print ("Incorrect Arguement Type")

if __name__ == '__main__':
    exCode = main(sys.argv[1])
    #print ('Exit Code: {code}'.format(code=exCode))
    sys.exit(exCode)
